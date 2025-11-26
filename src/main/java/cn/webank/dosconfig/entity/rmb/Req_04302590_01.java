package cn.webank.dosconfig.entity.rmb;

import java.util.List;

import cn.webank.dosconfig.entity.FieldOrder;
import cn.webank.dosconfig.entity.FilterCondition;
import cn.webank.dosconfig.entity.TimeDimension;
import cn.webank.weup.rmb.annotation.RmbCallTimeOut;
import cn.webank.weup.rmb.annotation.RmbField;
import cn.webank.weup.rmb.annotation.RmbMessage;
import cn.webank.weup.validate.annotations.WeupNotNull;
import cn.webank.weup.validate.annotations.collections.CollectionSizeRange;
import cn.webank.weup.validate.annotations.numbers.IntRange;

/**
 * 指标联机分析服务请求对象。
 */
@RmbMessage(
        name = "指标联机分析服务",
        serviceId = "04302590",
        scenarioId = "01",
        useDesc = "联机指标数据查询",
        threadPoolName = "rmb_onlineAnalysis"
)
@RmbCallTimeOut(120000)
public class Req_04302590_01 {

    @RmbField(seq = 1, title = "指标ID列表", remark = "指标ID列表")
    @WeupNotNull
    @CollectionSizeRange(minSize = 1, maxSize = 50)
    private List<String> metrics;

    @RmbField(seq = 2, title = "维度字段列表ID", remark = "维度字段列表ID")
    @CollectionSizeRange(maxSize = 15)
    private List<String> dimensions;

    @RmbField(seq = 3, title = "明细过滤条件", remark = "可选过滤条件")
    private FilterCondition filters;

    @RmbField(seq = 4, title = "时间维度过滤条件", remark = "时间维度过滤条件")
    @CollectionSizeRange(maxSize = 15)
    private List<TimeDimension> timeDimensions;

    @RmbField(seq = 5, title = "排序方式", remark = "排序方式")
    @CollectionSizeRange(maxSize = 15)
    private List<FieldOrder> sort;

    @RmbField(seq = 6, title = "限制返回条数", remark = "不超过10000条")
    @WeupNotNull
    @IntRange(min = 1, max = 10000)
    private Integer limit;

    public List<String> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<String> metrics) {
        this.metrics = metrics;
    }

    public List<String> getDimensions() {
        return dimensions;
    }

    public void setDimensions(List<String> dimensions) {
        this.dimensions = dimensions;
    }

    public FilterCondition getFilters() {
        return filters;
    }

    public void setFilters(FilterCondition filters) {
        this.filters = filters;
    }

    public List<TimeDimension> getTimeDimensions() {
        return timeDimensions;
    }

    public void setTimeDimensions(List<TimeDimension> timeDimensions) {
        this.timeDimensions = timeDimensions;
    }

    public List<FieldOrder> getSort() {
        return sort;
    }

    public void setSort(List<FieldOrder> sort) {
        this.sort = sort;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    @Override
    public String toString() {
        return "Req_04302590_01{" +
                "metrics=" + metrics +
                ", dimensions=" + dimensions +
                ", filters=" + filters +
                ", timeDimensions=" + timeDimensions +
                ", sort=" + sort +
                ", limit=" + limit +
                '}';
    }
}

