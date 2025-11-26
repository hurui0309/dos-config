package cn.webank.dosconfig.entity;

import java.util.Map;

/**
 * 明细过滤条件封装，占位实现。
 */
public class FilterCondition {

    /**
     * 过滤表达式，如 field_a = :paramA AND field_b > :paramB
     */
    private String expression;

    /**
     * 表达式绑定变量。
     */
    private Map<String, Object> params;

    public FilterCondition() {
    }

    public FilterCondition(String expression, Map<String, Object> params) {
        this.expression = expression;
        this.params = params;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }
}

