package cn.webank.dosconfig.entity.attribution.dto.response;

import java.math.BigDecimal;

/**
 * 维度归因明细（包含基准值 / 对比值 / 贡献度等关键指标）。
 */
public record  DimensionAttributionItemDTO(
        String dimension,
        String dimensionValue,
        BigDecimal compareValue,
        BigDecimal baselineValue,
        BigDecimal deltaValue,
        BigDecimal contribution,
        BigDecimal surprise,
        Integer rank
) {
}

