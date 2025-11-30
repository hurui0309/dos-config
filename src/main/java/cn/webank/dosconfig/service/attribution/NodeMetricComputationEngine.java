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
    private final Map<OperationType, OperationDeltaStrategy> deltaStrategies = new EnumMap<>(OperationType.class);
    private final OperationDeltaStrategy defaultDeltaStrategy = new DifferenceDeltaStrategy();

    public NodeMetricComputationEngine(BigDecimal epsilon) {
        this.epsilon = epsilon;
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
        OperationType operationType = Optional.ofNullable(node.op())
                .map(String::toUpperCase)
                .map(OperationType::valueOf)
                .orElse(OperationType.ADD);

        String metricId = node.metricId();
        MetricValue selfMetricValue = Optional.ofNullable(metricId)
                .map(metricValues::get)
                .orElseThrow(() -> new IllegalStateException("缺少指标值: " + node.nodeId()));
        baselineValue = selfMetricValue.baselineValue();
        compareValue = selfMetricValue.compareValue();

        // 3. 根据节点类型选择合适的增量拆解策略
        OperationDeltaStrategy deltaStrategy = deltaStrategies.getOrDefault(operationType, defaultDeltaStrategy);
        BigDecimal deltaValue = deltaStrategy.computeDelta(baselineValue, compareValue, children);

        logContribution(operationType, node.nodeId(), baselineValue, compareValue, deltaValue);
        BigDecimal denominator = baselineValue.abs().max(epsilon);
        BigDecimal deltaRate = denominator.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : deltaValue.divide(denominator, MathContext.DECIMAL64);

        return new NodeComputation(node, compareValue, baselineValue, deltaValue, deltaRate, children);
    }

    private void logContribution(OperationType operationType,
                                 String nodeId,
                                 BigDecimal baselineValue,
                                 BigDecimal compareValue,
                                 BigDecimal deltaValue) {
        if (!LOG.isInfoEnabled()) {
            return;
        }
        LOG.info("{}节点贡献: nodeId={}, baseline={}, compare={}, delta={}",
                operationType.name(), nodeId, baselineValue, compareValue, deltaValue);
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
            return baselineValue.subtract(compareValue);
        }
    }

    private class SubtractionDeltaStrategy implements OperationDeltaStrategy {
        @Override
        public BigDecimal computeDelta(BigDecimal baselineValue,
                                       BigDecimal compareValue,
                                       List<NodeComputation> children) {
            return baselineValue.subtract(compareValue);
        }
    }

    private class LmdiDeltaStrategy implements OperationDeltaStrategy {
        @Override
        public BigDecimal computeDelta(BigDecimal baselineValue,
                                       BigDecimal compareValue,
                                       List<NodeComputation> children) {
            if (children == null || children.isEmpty() || compareValue.compareTo(baselineValue) == 0) {
                return baselineValue.subtract(compareValue);
            }
            BigDecimal logMeanValue = logMean(baselineValue, compareValue);
            BigDecimal logChangeSum = BigDecimal.ZERO;
            for (NodeComputation child : children) {
                BigDecimal childBaseline = sanitizeValue(child.baselineValue());
                BigDecimal childCompare = sanitizeValue(child.compareValue());
                BigDecimal childLogChange = naturalLog(childCompare).subtract(naturalLog(childBaseline), MathContext.DECIMAL64);
                logChangeSum = logChangeSum.add(childLogChange, MathContext.DECIMAL64);
                BigDecimal childContribution = logMeanValue.multiply(childLogChange, MathContext.DECIMAL64);
                LOG.info("LMDI子项贡献: childNodeId={}, childName={}, baseline={}, compare={}, contribution={}",
                        child.node().nodeId(), child.node().nodeName(), childBaseline, childCompare, childContribution);
            }
            return logMeanValue.multiply(logChangeSum, MathContext.DECIMAL64).negate();
        }
    }

    private class RatioDeltaStrategy implements OperationDeltaStrategy {
        @Override
        public BigDecimal computeDelta(BigDecimal baselineValue,
                                       BigDecimal compareValue,
                                       List<NodeComputation> children) {
            if (children == null || children.size() < 2) {
                return baselineValue.subtract(compareValue);
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
            LOG.info("比值拆解子项: numeratorId={}, numeratorName={}, denominatorId={}, denominatorName={}, numeratorContribution={}, denominatorContribution={}",
                    numerator.node().nodeId(), numerator.node().nodeName(), denominator.node().nodeId(), denominator.node().nodeName(),
                    numeratorContribution, denominatorContribution);
            return logMeanValue.multiply(totalLogChange, MathContext.DECIMAL64).negate();
        }
    }
}

