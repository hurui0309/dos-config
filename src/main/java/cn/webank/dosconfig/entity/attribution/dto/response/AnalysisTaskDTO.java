package cn.webank.dosconfig.entity.attribution.dto.response;

import cn.webank.dosconfig.enums.DateGranularity;
import cn.webank.dosconfig.enums.TaskStatus;

import java.math.BigDecimal;

/**
 * 任务列表条目
 */
public record AnalysisTaskDTO(
        String taskId,
        String treeId,
        String treeName,
        String listId,
        String listName,
        BigDecimal contributionThreshold,
        DateGranularity timeGranularity,
        String baselineDate,
        String compareDate,
        TaskStatus status,
        Integer progress,
        String message,
        String creator,
        String createTime,
        String startTime,
        String endTime
) {
}

