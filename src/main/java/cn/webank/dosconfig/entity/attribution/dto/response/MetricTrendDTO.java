package cn.webank.dosconfig.entity.attribution.dto.response;

import java.util.List;

/**
 * 核心指标趋势响应（通用化设计）
 */
public record MetricTrendDTO(
        ChartDataDTO chartData,          // 图表数据（支持ECharts）
        List<MetricItemDTO> metricItems  // 关键指标列表（基准期均值、对比期均值、变化率等）
) {
}

