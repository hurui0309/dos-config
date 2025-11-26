package cn.webank.dosconfig.entity.attribution.dto.response;

/**
 * 归因树配置响应
 */
public record AttributionTreeConfigDTO(
        String treeId,
        String treeName,
        String metricId,
        String metricName,
        Integer version,
        MetricTreeNodeDTO treeConfig
) {
}

