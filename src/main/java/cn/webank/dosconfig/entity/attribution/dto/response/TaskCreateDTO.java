package cn.webank.dosconfig.entity.attribution.dto.response;

import cn.webank.dosconfig.enums.DateGranularity;

/**
 * 任务创建响应
 */
public record TaskCreateDTO(
        String taskId,
        DateGranularity timeGranularity,
        String baselineDate,
        String compareDate
) {
}

