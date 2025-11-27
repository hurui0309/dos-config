package cn.webank.dosconfig.service.attribution;

import cn.webank.dosconfig.entity.attribution.dto.response.MetricTreeNodeDTO;
import cn.webank.dosconfig.enums.DateGranularity;
import cn.webank.dosconfig.enums.OperationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 负责计算指标树中每个节点的基准值、对比值以及贡献度指标。
 */
public class NodeMetricComputationEngine {

    private static final Logger LOG = LoggerFactory.getLogger(NodeMetricComputationEngine.class);

    private final BigDecimal epsilon;
    private final Map<OperationType, MetricOperationStrategy> operationStrategies = new EnumMap<>(OperationType.class);
    private final Map<OperationType, OperationDeltaStrategy> deltaStrategies = new EnumMap<>(OperationType.class);
    private final OperationDeltaStrategy defaultDeltaStrategy = new DifferenceDeltaStrategy();

    public NodeMetricComputationEngine(BigDecimal epsilon) {
        this.epsilon = epsilon;
        operationStrategies.put(OperationType.ADD, this::aggregateAdd);
        operationStrategies.put(OperationType.SUB, this::aggregateSubtract);
        operationStrategies.put(OperationType.MUL, this::aggregateMultiply);
        operationStrategies.put(OperationType.DIV, this::aggregateDivide);

        deltaStrategies.put(OperationType.ADD, defaultDeltaStrategy);
        deltaStrategies.put(OperationType.SUB, new SubtractionDeltaStrategy());
        deltaStrategies.put(OperationType.MUL, new LmdiDeltaStrategy());
        deltaStrategies.put(OperationType.DIV, new RatioDeltaStrategy());
    }

    /**
     * 计算整棵指标树，返回包含所有节点指标值的树结构。
     * 会在遍历过程中根据节点运算类型（加/减/乘/除）对孩子节点进行聚合，
     * 其中「乘法」节点会使用 LMDI（Log-Mean Divisia Index）方式进行拆解，保证乘法关系也能被转化为可加性的增量。
     */
    public NodeComputation compute(MetricTreeNodeDTO root,
                                   LocalDate baselineDate,
                                   LocalDate compareDate,
                                   DateGranularity granularity,
                                   Map<String, MetricValue> metricValues) {
        LOG.info("节点指标值计算开始: rootNode={}, baseline={}, compare={}, granularity={}",
                root.nodeId(), baselineDate, compareDate, granularity);
        NodeComputation result = computeNode(root, metricValues);
        LOG.info("节点指标值计算完成: rootNode={}, baselineValue={}, compareValue={}",
                root.nodeId(), result.baselineValue, result.compareValue);
        return result;
    }

    /**
     * 深度优先计算单个节点：先计算子节点，再根据运算类型聚合得到当前节点的 baseline / compare / delta。
     */
    private NodeComputation computeNode(MetricTreeNodeDTO node, Map<String, MetricValue> metricValues) {
        List<NodeComputation> children = new ArrayList<>();
        if (node.children() != null) {
            for (MetricTreeNodeDTO child : node.children()) {
                children.add(computeNode(child, metricValues));
            }
        }

        BigDecimal baselineValue;
        BigDecimal compareValue;
        OperationType operationType;
        // 1. 叶子节点：直接取缓存的指标值
        if (children.isEmpty()) {
            MetricValue metricValue = metricValues.get(node.metricId());
            if (metricValue == null) {
                throw new IllegalStateException("缺少指标值: " + node.metricId());
            }
            baselineValue = metricValue.baselineValue();
            compareValue = metricValue.compareValue();
            operationType = OperationType.ADD;
        } else {
            // 2. 组合节点：根据配置的运算符聚合子节点
            operationType = Optional.ofNullable(node.op())
                    .map(String::toUpperCase)
                    .map(OperationType::valueOf)
                    .orElse(OperationType.ADD);
            baselineValue = aggregate(operationType, children, false);
            compareValue = aggregate(operationType, children, true);
        }

        // 3. 根据节点类型选择合适的增量拆解策略
        OperationDeltaStrategy deltaStrategy = deltaStrategies.getOrDefault(operationType, defaultDeltaStrategy);
        BigDecimal deltaValue = deltaStrategy.computeDelta(baselineValue, compareValue, children);

        BigDecimal denominator = baselineValue.abs().max(epsilon);
        BigDecimal deltaRate = denominator.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : deltaValue.divide(denominator, MathContext.DECIMAL64);

        return new NodeComputation(node, compareValue, baselineValue, deltaValue, deltaRate, children);
    }

    /**
     * 根据运算类型选择合适的聚合策略。
     */
    private BigDecimal aggregate(OperationType operationType, List<NodeComputation> children, boolean useCompareValue) {
        MetricOperationStrategy strategy = operationStrategies.getOrDefault(operationType, this::aggregateAdd);
        return strategy.aggregate(children, useCompareValue);
    }

    /**
     * 加法：所有子节点求和。
     */
    private BigDecimal aggregateAdd(List<NodeComputation> children, boolean useCompareValue) {
        return children.stream()
                .map(child -> useCompareValue ? child.compareValue : child.baselineValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 减法：第一个子节点减去其余子节点。
     */
    private BigDecimal aggregateSubtract(List<NodeComputation> children, boolean useCompareValue) {
        if (children.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal result = useCompareValue ? children.get(0).compareValue : children.get(0).baselineValue;
        for (int i = 1; i < children.size(); i++) {
            BigDecimal value = useCompareValue ? children.get(i).compareValue : children.get(i).baselineValue;
            result = result.subtract(value);
        }
        return result;
    }

    /**
     * 乘法节点聚合：计算所有子节点的乘积。
     * 乘法节点的增量拆解由 LMDI 负责，此处只负责求出对比期/基准期的「总量」。
     */
    private BigDecimal aggregateMultiply(List<NodeComputation> children, boolean useCompareValue) {
        if (children.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal product = BigDecimal.ONE;
        for (NodeComputation child : children) {
            BigDecimal value = useCompareValue ? child.compareValue : child.baselineValue;
            product = product.multiply(value, MathContext.DECIMAL64);
        }
        return product;
    }

    /**
     * 除法：第一个子节点 / 第二个子节点，其余忽略。
     */
    private BigDecimal aggregateDivide(List<NodeComputation> children, boolean useCompareValue) {
        if (children.size() < 2) {
            return BigDecimal.ZERO;
        }
        BigDecimal numerator = useCompareValue ? children.get(0).compareValue : children.get(0).baselineValue;
        BigDecimal denominator = useCompareValue ? children.get(1).compareValue : children.get(1).baselineValue;
        return denominator.abs().compareTo(epsilon) < 0
                ? BigDecimal.ZERO
                : numerator.divide(denominator, MathContext.DECIMAL64);
    }

    /**
     * 中间节点结果，包含指标值和子节点信息。
     */
    public static class NodeComputation {
        private final MetricTreeNodeDTO node;
        private final BigDecimal compareValue;
        private final BigDecimal baselineValue;
        private final BigDecimal deltaValue;
        private final BigDecimal deltaRate;
        private final List<NodeComputation> children;

        private NodeComputation(MetricTreeNodeDTO node,
                                BigDecimal compareValue,
                                BigDecimal baselineValue,
                                BigDecimal deltaValue,
                                BigDecimal deltaRate,
                                List<NodeComputation> children) {
            this.node = node;
            this.compareValue = compareValue;
            this.baselineValue = baselineValue;
            this.deltaValue = deltaValue;
            this.deltaRate = deltaRate;
            this.children = children;
        }

        public MetricTreeNodeDTO node() {
            return node;
        }

        public BigDecimal compareValue() {
            return compareValue;
        }

        public BigDecimal baselineValue() {
            return baselineValue;
        }

        public BigDecimal deltaValue() {
            return deltaValue;
        }

        public BigDecimal deltaRate() {
            return deltaRate;
        }

        public List<NodeComputation> children() {
            return children;
        }
    }

    public record MetricValue(BigDecimal baselineValue, BigDecimal compareValue) {
    }

    @FunctionalInterface
    private interface MetricOperationStrategy {
        BigDecimal aggregate(List<NodeComputation> children, boolean useCompareValue);
    }

    /**
     * 避免出现 0 或负数参与对数运算，统一替换为 epsilon。
     */
    private BigDecimal sanitizeValue(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return epsilon;
        }
        return value;
    }

    /**
     * 计算自然对数，统一使用 DECIMAL64 精度。
     */
    private BigDecimal naturalLog(BigDecimal value) {
        double result = Math.log(value.doubleValue());
        return new BigDecimal(result, MathContext.DECIMAL64);
    }

    /**
     * 计算对数均值 L(a, b)。
     */
    private BigDecimal logMean(BigDecimal a, BigDecimal b) {
        if (a.compareTo(b) == 0) {
            return a;
        }
        BigDecimal safeA = sanitizeValue(a);
        BigDecimal safeB = sanitizeValue(b);
        BigDecimal numerator = safeA.subtract(safeB, MathContext.DECIMAL64);
        BigDecimal denominator = naturalLog(safeA).subtract(naturalLog(safeB), MathContext.DECIMAL64);
        if (denominator.abs().compareTo(epsilon) < 0) {
            return BigDecimal.ZERO;
        }
        return numerator.divide(denominator, MathContext.DECIMAL64);
    }

    private interface OperationDeltaStrategy {
        BigDecimal computeDelta(BigDecimal baselineValue,
                                BigDecimal compareValue,
                                List<NodeComputation> children);
    }

    private class DifferenceDeltaStrategy implements OperationDeltaStrategy {
        @Override
        public BigDecimal computeDelta(BigDecimal baselineValue,
                                       BigDecimal compareValue,
                                       List<NodeComputation> children) {
            return compareValue.subtract(baselineValue);
        }
    }

    private class SubtractionDeltaStrategy implements OperationDeltaStrategy {
        @Override
        public BigDecimal computeDelta(BigDecimal baselineValue,
                                       BigDecimal compareValue,
                                       List<NodeComputation> children) {
            if (children == null || children.isEmpty()) {
                return compareValue.subtract(baselineValue);
            }
            BigDecimal delta = BigDecimal.ZERO;
            for (int i = 0; i < children.size(); i++) {
                NodeComputation child = children.get(i);
                BigDecimal childDelta = child.compareValue().subtract(child.baselineValue(), MathContext.DECIMAL64);
                delta = delta.add(i == 0 ? childDelta : childDelta.negate(), MathContext.DECIMAL64);
            }
            return delta;
        }
    }

    private class LmdiDeltaStrategy implements OperationDeltaStrategy {
        @Override
        public BigDecimal computeDelta(BigDecimal baselineValue,
                                       BigDecimal compareValue,
                                       List<NodeComputation> children) {
            if (children == null || children.isEmpty() || compareValue.compareTo(baselineValue) == 0) {
                return compareValue.subtract(baselineValue);
            }
            BigDecimal logMeanValue = logMean(baselineValue, compareValue);
            BigDecimal logChangeSum = BigDecimal.ZERO;
            for (NodeComputation child : children) {
                BigDecimal childBaseline = sanitizeValue(child.baselineValue());
                BigDecimal childCompare = sanitizeValue(child.compareValue());
                BigDecimal childLogChange = naturalLog(childCompare).subtract(naturalLog(childBaseline), MathContext.DECIMAL64);
                logChangeSum = logChangeSum.add(childLogChange, MathContext.DECIMAL64);
                BigDecimal childContribution = logMeanValue.multiply(childLogChange, MathContext.DECIMAL64);
                LOG.debug("LMDI 子项贡献: nodeId={}, childId={}, baseline={}, compare={}, contribution={}",
                        child.node().nodeId(), child.node().nodeName(), childBaseline, childCompare, childContribution);
            }
            return logMeanValue.multiply(logChangeSum, MathContext.DECIMAL64);
        }
    }

    private class RatioDeltaStrategy implements OperationDeltaStrategy {
        @Override
        public BigDecimal computeDelta(BigDecimal baselineValue,
                                       BigDecimal compareValue,
                                       List<NodeComputation> children) {
            if (children == null || children.size() < 2) {
                return compareValue.subtract(baselineValue);
            }
            NodeComputation numerator = children.get(0);
            NodeComputation denominator = children.get(1);
            BigDecimal numeratorBaseline = sanitizeValue(numerator.baselineValue());
            BigDecimal numeratorCompare = sanitizeValue(numerator.compareValue());
            BigDecimal denominatorBaseline = sanitizeValue(denominator.baselineValue());
            BigDecimal denominatorCompare = sanitizeValue(denominator.compareValue());

            BigDecimal logMeanValue = logMean(baselineValue, compareValue);
            BigDecimal numeratorLogChange = naturalLog(numeratorCompare).subtract(naturalLog(numeratorBaseline), MathContext.DECIMAL64);
            BigDecimal denominatorLogChange = naturalLog(denominatorCompare).subtract(naturalLog(denominatorBaseline), MathContext.DECIMAL64);
            BigDecimal totalLogChange = numeratorLogChange.subtract(denominatorLogChange, MathContext.DECIMAL64);

            BigDecimal numeratorContribution = logMeanValue.multiply(numeratorLogChange, MathContext.DECIMAL64);
            BigDecimal denominatorContribution = logMeanValue.multiply(denominatorLogChange.negate(), MathContext.DECIMAL64);
            LOG.debug("比值拆解: nodeId={}, numerator={}, denominator={}, numContr={}, denContr={}",
                    numerator.node().nodeId(), numerator.node().nodeName(), denominator.node().nodeName(),
                    numeratorContribution, denominatorContribution);
            return logMeanValue.multiply(totalLogChange, MathContext.DECIMAL64);
        }
    }
}

