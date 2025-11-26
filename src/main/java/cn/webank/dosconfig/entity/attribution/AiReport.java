package cn.webank.dosconfig.entity.attribution;

import cn.webank.dosconfig.enums.ReportStatus;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI归因报告实体类
 */
@Data
public class AiReport {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 报告生成状态
     */
    private ReportStatus reportStatus = ReportStatus.GENERATING;

    /**
     * 报告内容（大TEXT，完整报告内容）
     */
    private String reportContent;

    /**
     * 生成时间
     */
    private LocalDateTime generateTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}

