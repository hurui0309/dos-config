package cn.webank.dosconfig.entity.attribution.dto.response;

/**
 * 指标简要信息响应
 */
public record MetricBriefDTO(
        String metricId,
        String metricName,
        String treeId,
        String treeName
) {
}

