package cn.webank.dosconfig.entity.attribution.dto.response;

import cn.webank.dosconfig.enums.TaskStatus;

/**
 * 任务状态响应
 */
public record TaskStatusDTO(
        String taskId,
        TaskStatus status,
        Integer progress,
        String message,
        String createTime,
        String startTime,
        String endTime
) {
}

