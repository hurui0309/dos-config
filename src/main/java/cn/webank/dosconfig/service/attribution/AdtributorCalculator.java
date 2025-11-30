package cn.webank.dosconfig.service.attribution;

import cn.webank.dosconfig.entity.attribution.dto.response.DimensionAttributionItemDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Adtributor 算法 Java 版本，参考仓库内 Python 实现。
 * 负责在单个维度下，基于 Surprise / EP 指标筛选出贡献最大的维度取值。
 */
public class AdtributorCalculator {

    private static final Logger LOG = LoggerFactory.getLogger(AdtributorCalculator.class);

    private final BigDecimal epsilon;

    private final MathContext mathContext = new MathContext(8, RoundingMode.HALF_UP);

    public AdtributorCalculator(BigDecimal epsilon) {
        this.epsilon = epsilon;
    }

    /**
     * 执行单维归因算法，返回贡献度 TopN 的维度值列表。
     * 计算步骤：统计增量、计算 EP 和 Surprise、按贡献排序并应用阈值过滤，最终返回包含贡献度与排名信息的集合。
     *
     * @param dimensionId       维度唯一标识
     * @param compareValues     对比期按维度聚合的指标值
     * @param baselineValues    基准期按维度聚合的指标值
     * @param epThreshold       单个维度最小 EP 阈值
     * @param epTotalThreshold  EP 累计阈值
     * @param maxResultSize     返回的最大条数
     * @return 维度归因结果列表
     */
    public List<DimensionAttributionItemDTO> calculate(String dimensionId,
                                                       Map<String, BigDecimal> compareValues,
                                                       Map<String, BigDecimal> baselineValues,
                                                       BigDecimal epThreshold,
                                                       BigDecimal epTotalThreshold,
                                                       int maxResultSize) {
        LOG.info("Adtributor计算开始: dimensionId={}, compareSize={}, baselineSize={}",
                dimensionId, compareValues.size(), baselineValues.size());
        BigDecimal totalCompare = sum(compareValues);
        BigDecimal totalBaseline = sum(baselineValues);
        BigDecimal totalDelta = totalBaseline.subtract(totalCompare);

        List<DimensionCandidate> candidates = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : compareValues.entrySet()) {
            String value = entry.getKey();
            BigDecimal compare = entry.getValue();
            BigDecimal baseline = baselineValues.getOrDefault(value, BigDecimal.ZERO);
            BigDecimal delta = baseline.subtract(compare);
            BigDecimal ep = safeDivide(delta, totalDelta);
            BigDecimal surprise = surprise(compare, baseline, totalCompare, totalBaseline);
            candidates.add(new DimensionCandidate(value, compare, baseline, delta, ep, surprise));
        }

        // baseline 中独有的项也需要考虑
        for (Map.Entry<String, BigDecimal> entry : baselineValues.entrySet()) {
            if (compareValues.containsKey(entry.getKey())) {
                continue;
            }
            String value = entry.getKey();
            BigDecimal baseline = entry.getValue();
            BigDecimal compare = BigDecimal.ZERO;
            BigDecimal delta = baseline.subtract(compare);
            BigDecimal ep = safeDivide(delta, totalDelta);
            BigDecimal surprise = surprise(compare, baseline, totalCompare, totalBaseline);
            candidates.add(new DimensionCandidate(value, compare, baseline, delta, ep, surprise));
        }

        candidates.sort(Comparator.comparing(DimensionCandidate::surprise).reversed());

        List<DimensionAttributionItemDTO> results = new ArrayList<>();
        BigDecimal accumulatedEp = BigDecimal.ZERO;
        int rank = 1;
        for (DimensionCandidate candidate : candidates) {
            if (candidate.ep.abs().compareTo(epThreshold) < 0) {
                continue;
            }
            accumulatedEp = accumulatedEp.add(candidate.ep.abs());
            // TODO jiahao: 后续版本需要处理指标值为负的场景
            results.add(new DimensionAttributionItemDTO(
                    dimensionId,
                    candidate.value,
                    candidate.compareValue,
                    candidate.baselineValue,
                    candidate.deltaValue,
                    candidate.ep,
                    candidate.surprise,
                    rank++
            ));
            if (results.size() >= maxResultSize || accumulatedEp.compareTo(epTotalThreshold) >= 0) {
                break;
            }
        }
        LOG.info("Adtributor计算完成: dimensionId={}, resultSize={}, totalDelta={}",
                dimensionId, results.size(), totalDelta);
        return results;
    }

    private BigDecimal sum(Map<String, BigDecimal> values) {
        return values.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal safeDivide(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.abs().compareTo(epsilon) < 0) {
            return BigDecimal.ZERO;
        }
        return numerator.divide(denominator, mathContext);
    }

    private BigDecimal surprise(BigDecimal compare, BigDecimal baseline, BigDecimal totalCompare, BigDecimal totalBaseline) {
        if (totalCompare.abs().compareTo(epsilon) < 0 || totalBaseline.abs().compareTo(epsilon) < 0) {
            return BigDecimal.ZERO;
        }
        double p = compare.doubleValue() / totalCompare.doubleValue();
        double q = baseline.doubleValue() / totalBaseline.doubleValue();
        if (closeToZero(p) && closeToZero(q)) {
            return BigDecimal.ZERO;
        }
        double surprise;
        if (closeToZero(p)) {
            surprise = 0.5 * q * Math.log10(2);
        } else if (closeToZero(q)) {
            surprise = 0.5 * p * Math.log10(2);
        } else {
            surprise = 0.5 * (p * log10(2 * p / (p + q)) + q * log10(2 * q / (p + q)));
        }
        return new BigDecimal(surprise, mathContext);
    }

    private boolean closeToZero(double value) {
        return Math.abs(value) < epsilon.doubleValue();
    }

    private double log10(double value) {
        return Math.log(value) / Math.log(10);
    }

    private record DimensionCandidate(String value,
                                      BigDecimal compareValue,
                                      BigDecimal baselineValue,
                                      BigDecimal deltaValue,
                                      BigDecimal ep,
                                      BigDecimal surprise) {
    }
}

