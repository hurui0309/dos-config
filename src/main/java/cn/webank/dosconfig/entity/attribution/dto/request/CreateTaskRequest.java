package cn.webank.dosconfig.entity.attribution.dto.request;

import cn.webank.dosconfig.enums.DateGranularity;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 创建分析任务请求
 */
public record CreateTaskRequest(
        String treeId,
        String listId,
        BigDecimal contributionThreshold,
        DateGranularity timeGranularity,
        String baselineDate,
        String compareDate
) {

    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    public CreateTaskRequest {
        requireNonBlank(treeId, "归因树ID不能为空");
        Objects.requireNonNull(contributionThreshold, "贡献度阈值不能为空");
        if (contributionThreshold.compareTo(BigDecimal.ZERO) < 0 || contributionThreshold.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("贡献度阈值必须在0到1之间");
        }

        Objects.requireNonNull(timeGranularity, "时间粒度不能为空");

        requireDate(baselineDate, "基准日期不能为空");
        requireDate(compareDate, "对比日期不能为空");
    }

    private static void requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireDate(String value, String message) {
        requireNonBlank(value, message);
        if (!DATE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("日期格式必须为yyyy-MM-dd");
        }
    }
}
