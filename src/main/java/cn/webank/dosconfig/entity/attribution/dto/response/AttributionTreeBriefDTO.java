package cn.webank.dosconfig.entity.attribution.dto.response;

/**
 * 归因树简要信息响应
 */
public record AttributionTreeBriefDTO(
        String treeId,
        String treeName,
        String metricId,
        String metricName,
        Integer version
) {
}

