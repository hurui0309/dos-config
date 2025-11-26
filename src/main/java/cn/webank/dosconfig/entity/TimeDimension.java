package cn.webank.dosconfig.entity;

import java.util.List;

import cn.webank.dosconfig.enums.DateGranularity;
import cn.webank.weup.validate.annotations.WeupNotAnyEmpty;
import cn.webank.weup.validate.annotations.WeupNotNull;
import cn.webank.weup.validate.annotations.TextOfEnum;
import cn.webank.weup.validate.annotations.collections.CollectionSizeRange;

/**
 * 时间维度配置，占位实现，兼容 RMB 请求。
 */
public class TimeDimension {

    /**
     * 维度字段 ID，默认按日。
     */
    @WeupNotAnyEmpty
    private String dimension = "dim_calendar_a.fmt_date";

    /**
     * 时间粒度。
     */
    @TextOfEnum(enumClass = DateGranularity.class)
    private String granularity = "day";

    /**
     * 日期筛选范围，两端闭区间。
     */
    @WeupNotNull
    @CollectionSizeRange(minSize = 2, maxSize = 2)
    private List<String> dateRange;

    public TimeDimension() {
    }

    public TimeDimension(String dimension, String granularity, List<String> dateRange) {
        this.dimension = dimension;
        this.granularity = granularity;
        this.dateRange = dateRange;
    }

    public String getDimension() {
        return dimension;
    }

    public void setDimension(String dimension) {
        this.dimension = dimension;
    }

    public String getGranularity() {
        return granularity;
    }

    public void setGranularity(String granularity) {
        this.granularity = granularity;
    }

    public List<String> getDateRange() {
        return dateRange;
    }

    public void setDateRange(List<String> dateRange) {
        this.dateRange = dateRange;
    }
}

