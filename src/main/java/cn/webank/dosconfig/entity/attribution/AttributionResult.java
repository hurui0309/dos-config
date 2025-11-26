package cn.webank.dosconfig.entity.attribution;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 归因分析结果实体类
 */
@Data
public class AttributionResult {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 结果树（JSON格式）
     */
    private String resultTree;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}

