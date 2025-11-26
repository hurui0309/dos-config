package cn.webank.dosconfig.entity.attribution.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * 归因树结果节点（递归）
 */
public record  AttributionTreeResultNodeDTO(
        String nodeId,
        String nodeName,
        String metricId,
        Boolean isRate,
        String op,
        BigDecimal currentValue,
        BigDecimal baselineValue,
        BigDecimal deltaValue,
        BigDecimal deltaRate,
        BigDecimal contributionLocal,
        BigDecimal contributionGlobal,
        List<DimensionAttributionItemDTO> dimensionAttribution,
        List<AttributionTreeResultNodeDTO> children
) {
}

