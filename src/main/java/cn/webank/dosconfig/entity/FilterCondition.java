package cn.webank.dosconfig.entity;

import cn.webank.dosconfig.enums.EOperator;
import cn.webank.weup.base.util.JSONUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author markliu
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FilterCondition {

    public static final String RELATION_AND = "AND";

    public static final String RELATION_OR = "OR";

    public static final String RELATION_NOT = "NOT";

    public static final String RELATION_NODE = "relation";

    public static final String FIELD_NODE = "field";

    private String nodeType;

    private String relation;

    private String field;

    private String op;

    private Object value;

    private List<FilterCondition> subConditions;

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        if (op == null) {
            this.op = null;
            return;
        }
        EOperator operator = EOperator.fromString(op);
        this.op = operator != null ? operator.name() : op;
    }

    public List<FilterCondition> getSubConditions() {
        return subConditions;
    }

    public void setSubConditions(List<FilterCondition> subConditions) {
        this.subConditions = subConditions;
    }

    @JsonIgnore
    public boolean isEmpty() {
        boolean isRelation = FilterCondition.RELATION_NODE.equalsIgnoreCase(nodeType);
        if (isRelation) {
            if (subConditions == null || subConditions.isEmpty()) {
                return true;
            }
            for (FilterCondition child : subConditions) {
                if (child != null && !child.isEmpty()) {
                    return false;
                }
            }
            return true;
        }
        return StringUtils.isEmpty(field);
    }

    public static FilterCondition operation(String field, EOperator op, Object value) {
        if (StringUtils.isEmpty(field)) {
            throw new IllegalArgumentException("field不能为空");
        }

        FilterCondition condition = new FilterCondition();
        condition.setNodeType(FIELD_NODE);
        condition.setField(field);
        condition.setOp(op.toString());
        condition.setValue(value);
        return condition;
    }

    public static FilterCondition and(String field, EOperator op, Object value, FilterCondition filter) {
        FilterCondition operation = operation(field, op, value);
        List<FilterCondition> conditionList = Arrays.asList(operation, filter);
        return and(conditionList);
    }

    public static FilterCondition and(List<FilterCondition> conditionList) {
        FilterCondition condition = new FilterCondition();
        condition.setNodeType(RELATION_NODE);
        condition.setRelation(RELATION_AND);
        condition.setSubConditions(conditionList);
        return condition;
    }

    public static FilterCondition and(Map<String, Object> kvMap) {
        Set<Map.Entry<String, Object>> entrySet = kvMap.entrySet();
        List<FilterCondition> condList = new ArrayList<>(kvMap.size());

        for (Map.Entry<String, Object> entry : entrySet) {
            condList.add(operation(entry.getKey(), EOperator.EQ, entry.getValue()));
        }

        return and(condList);
    }

    public static FilterCondition or(List<FilterCondition> conditionList) {
        FilterCondition condition = new FilterCondition();
        condition.setNodeType(RELATION_NODE);
        condition.setRelation(RELATION_OR);
        condition.setSubConditions(conditionList);
        return condition;
    }

    public static FilterCondition or(Map<String, Object> kvMap) {
        Set<Map.Entry<String, Object>> entrySet = kvMap.entrySet();
        List<FilterCondition> condList = new ArrayList<>(kvMap.size());
        for (Map.Entry<String, Object> entry : entrySet) {
            condList.add(operation(entry.getKey(), EOperator.EQ, entry.getValue()));
        }

        return or(condList);
    }

    public static FilterCondition fromJson(String json) {
        return JSONUtil.fromJsonStr(json, FilterCondition.class);
    }

    public static String toJson(FilterCondition condition) {
        return JSONUtil.toDenseJsonStr(condition);
    }

    public static List<String> getFields(FilterCondition condition) {
        List<String> fields = new ArrayList<>();
        if (condition != null) {
            if (StringUtils.isNotEmpty(condition.getField())) {
                fields.add(condition.getField());
            }
            if (condition.getSubConditions() != null && !condition.getSubConditions().isEmpty()) {
                condition.getSubConditions().forEach(c -> fields.addAll(getFields(c)));
            }
        }
        return fields.stream().distinct().collect(Collectors.toList());
    }

    public static List<String> parseFilters(FilterCondition filters) {
        List<String> fieldList = new ArrayList<>();
        if (filters != null) {
            getFilterFields(filters, fieldList);
        }
        return fieldList;
    }

    private static void getFilterFields(FilterCondition condition, List<String> fieldList) {
        if (condition == null || condition.isEmpty()) {
            return;
        }
        String nodeType = condition.getNodeType();
        if (FilterCondition.RELATION_NODE.equalsIgnoreCase(nodeType)) {
            parseRelation(condition, fieldList);
        } else if (FilterCondition.FIELD_NODE.equalsIgnoreCase(nodeType)) {
            fieldList.add(condition.getField());
        }
    }

    private static void parseRelation(FilterCondition condition, List<String> fieldList) {
        List<FilterCondition> subConditions = condition.getSubConditions();
        if (subConditions == null || subConditions.isEmpty()) {
            return;
        }
        for (FilterCondition expr : subConditions) {
            if (!expr.isEmpty()) {
                getFilterFields(expr, fieldList);
            }
        }
    }

    @Override
    public String toString() {
        return toJson(this);
    }
}
