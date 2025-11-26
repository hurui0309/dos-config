package cn.webank.dosconfig.entity.attribution.dto.response;

import cn.webank.dosconfig.enums.ReportStatus;

/**
 * AI归因报告响应
 */
public record AiAttributionReportDTO(
        String taskId,
        ReportStatus reportStatus,
        String reportContent,  // 完整报告内容（大TEXT）
        String generateTime
) {
}

