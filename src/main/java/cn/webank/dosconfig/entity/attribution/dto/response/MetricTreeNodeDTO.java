package cn.webank.dosconfig.entity.attribution.dto.response;

import java.util.List;
import java.util.Map;

/**
 * 指标树节点（配置）
 */
public record  MetricTreeNodeDTO(
        String nodeId,
        String nodeName,
        String metricId,
        Boolean isRate,
        String op,
        List<String> dimensions,
        Map<String, Object> params,
        List<MetricTreeNodeDTO> children
) {
}

