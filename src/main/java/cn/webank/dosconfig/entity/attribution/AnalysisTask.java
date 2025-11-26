package cn.webank.dosconfig.entity.attribution;

import cn.webank.dosconfig.enums.DateGranularity;
import cn.webank.dosconfig.enums.TaskStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 分析任务实体类
 */
@Data
public class AnalysisTask {

    /**
     * 任务唯一标识
     */
    private String taskId;

    /**
     * 归因树ID
     */
    private String treeId;

    /**
     * 归因树名称（冗余字段，便于查询）
     */
    private String treeName;

    /**
     * 名单ID（可选）
     */
    private String listId;

    /**
     * 名单名称（冗余字段）
     */
    private String listName;

    /**
     * 贡献度阈值
     */
    private BigDecimal contributionThreshold;

    /**
     * 时间粒度
     */
    private DateGranularity timeGranularity;

    /**
     * 基准日期
     */
    private LocalDate baselineDate;

    /**
     * 对比日期
     */
    private LocalDate compareDate;

    /**
     * 任务状态
     */
    private TaskStatus status = TaskStatus.PENDING;

    /**
     * 执行进度（0-100）
     */
    private Integer progress = 0;

    /**
     * 状态说明
     */
    private String message;

    /**
     * 创建人
     */
    private String creator;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}

