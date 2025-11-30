package cn.webank.dosconfig.entity.attribution;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 归因树配置实体类
 */
@Data
public class AttributionTree {

    /**
     * 归因树唯一标识
     */
    private String treeId;

    /**
     * 归因树中文名称
     */
    private String treeName;

    /**
     * 指标ID
     */
    private String metricId;

    /**
     * 指标中文名称
     */
    private String metricName;

    /**
     * 配置版本号
     */
    private Integer version = 1;

    /**
     * 树结构配置（JSON格式）
     */
    private String treeConfig;

    /**
     * 全局过滤条件（JSON格式，FilterCondition）
     */
    private String globalFilter;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}

