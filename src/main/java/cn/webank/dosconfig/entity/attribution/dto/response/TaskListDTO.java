package cn.webank.dosconfig.entity.attribution.dto.response;

import java.util.List;

/**
 * 任务列表响应
 */
public record TaskListDTO(
        Integer total,
        Integer pageNo,
        Integer pageSize,
        List<AnalysisTaskDTO> tasks
) {
}

