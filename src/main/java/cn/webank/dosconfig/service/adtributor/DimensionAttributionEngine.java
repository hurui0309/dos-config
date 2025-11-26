package cn.webank.dosconfig.service.adtributor;

import cn.webank.dosconfig.entity.attribution.dto.response.DimensionAttributionItemDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 封装维度归因算法调用，基于 Adtributor 挑选贡献度最高的维度取值。
 */
public class DimensionAttributionEngine {

    private static final Logger LOG = LoggerFactory.getLogger(DimensionAttributionEngine.class);

    private final AdtributorCalculator adtributorCalculator;
    private final int dimensionMaxResult;
    private final BigDecimal dimensionEpThreshold;
    private final BigDecimal dimensionEpTotalThreshold;

    public DimensionAttributionEngine(BigDecimal epsilon,
                                      int dimensionMaxResult,
                                      BigDecimal dimensionEpThreshold,
                                      BigDecimal dimensionEpTotalThreshold) {
        this.adtributorCalculator = new AdtributorCalculator(epsilon);
        this.dimensionMaxResult = dimensionMaxResult;
        this.dimensionEpThreshold = dimensionEpThreshold;
        this.dimensionEpTotalThreshold = dimensionEpTotalThreshold;
    }

    /**
     * 针对单个维度执行归因计算，返回贡献度明细列表。
     *
     * @param dimensionId    维度 ID
     * @param compareValues  对比期维度取值
     * @param baselineValues 基准期维度取值
     * @return 维度归因明细
     */
    public List<DimensionAttributionItemDTO> analyze(String dimensionId,
                                                     Map<String, BigDecimal> compareValues,
                                                     Map<String, BigDecimal> baselineValues) {
        LOG.info("维度归因计算: dimensionId={}, compareSize={}, baselineSize={}",
                dimensionId, compareValues.size(), baselineValues.size());
        return adtributorCalculator.calculate(
                dimensionId,
                compareValues,
                baselineValues,
                dimensionEpThreshold,
                dimensionEpTotalThreshold,
                dimensionMaxResult
        );
    }
}

