package cn.webank.dosconfig.entity.attribution.dto.response;

import java.util.List;

/**
 * 图表数据DTO（通用化设计，支持ECharts）
 */
public record ChartDataDTO(
        XAxisDTO xAxis,
        YAxisDTO yAxis,
        List<SeriesDTO> series
) {
    /**
     * X轴配置
     */
    public record XAxisDTO(
            String type,  // 'category' 或 'value'
            List<String> data  // X轴数据
    ) {}
    
    /**
     * Y轴配置
     */
    public record YAxisDTO(
            String type  // 'value' 或 'category'
    ) {}
    
    /**
     * 系列数据
     */
    public record SeriesDTO(
            String name,        // 系列名称（如：基准期、对比期）
            String type,        // 图表类型（line、bar等）
            List<Object> data   // 数据数组
    ) {}
}

