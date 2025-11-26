package cn.webank.dosconfig.entity.attribution.dto.response;

import java.math.BigDecimal;

/**
 * 维度归因明细
 */
public record  DimensionAttributionItemDTO(
        String dimension,
        String dimensionValue,
        BigDecimal currentValue,
        BigDecimal baselineValue,
        BigDecimal deltaValue,
        BigDecimal contribution,
        BigDecimal surprise,
        Integer rank
) {
}

