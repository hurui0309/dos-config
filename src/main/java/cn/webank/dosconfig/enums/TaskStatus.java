package cn.webank.dosconfig.enums;

/**
 * 任务状态枚举
 */
public enum TaskStatus {
    /**
     * 待执行
     */
    PENDING,

    /**
     * 执行中
     */
    RUNNING,

    /**
     * 已完成（成功）
     */
    SUCCESS,

    /**
     * 失败
     */
    FAILED,

    /**
     * 已取消
     */
    CANCELED
}

