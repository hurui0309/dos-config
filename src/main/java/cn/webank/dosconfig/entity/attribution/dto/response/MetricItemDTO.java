package cn.webank.dosconfig.entity.attribution.dto.response;

/**
 * 指标项DTO（用于展示关键指标）
 */
public record MetricItemDTO(
        String key,      // 指标键（如：baselineAvg）
        String value,    // 指标值（如：225.5）
        String title,    // 展示标题（如：基准期均值）
        String color,    // 颜色（red、green、blue等）
        String remark    // 备注说明
) {}

