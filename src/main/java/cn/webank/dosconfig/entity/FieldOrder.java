package cn.webank.dosconfig.entity;

/**
 * 指标查询的排序定义。
 */
public class FieldOrder {

    /**
     * 排序字段 ID。
     */
    private String field;

    /**
     * 排序方式，asc/desc。
     */
    private String order;

    public FieldOrder() {
    }

    public FieldOrder(String field, String order) {
        this.field = field;
        this.order = order;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }
}

