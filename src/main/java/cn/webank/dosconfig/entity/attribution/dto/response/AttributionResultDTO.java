package cn.webank.dosconfig.entity.attribution.dto.response;

import cn.webank.dosconfig.enums.DateGranularity;

/**
 * 归因分析结果响应
 */
public record AttributionResultDTO(
        String taskId,
        String treeId,
        String treeName,
        DateGranularity timeGranularity,
        String baselineDate,
        String compareDate,
        AttributionTreeResultNodeDTO resultTree
) {
}

