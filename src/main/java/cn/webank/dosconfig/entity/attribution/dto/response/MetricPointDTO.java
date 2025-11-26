package cn.webank.dosconfig.entity.attribution.dto.response;

import java.math.BigDecimal;

/**
 * 指标时序点
 */
public record  MetricPointDTO(
        String date,
        BigDecimal value
) {
}

