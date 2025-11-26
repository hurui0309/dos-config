package cn.webank.weup.biz.rmb;

import java.util.Objects;

import cn.webank.weup.biz.threadpool.WeupThreadPoolTaskExecutor;

/**
 * 迪奥 Weup RMB 工具占位，仅透传异步执行。
 */
public final class WeupRmbUtil {

    private WeupRmbUtil() {
    }

    public static void asyncRunWithContext(WeupThreadPoolTaskExecutor executor, Runnable runnable) {
        Objects.requireNonNull(executor, "executor must not be null");
        Objects.requireNonNull(runnable, "runnable must not be null");
        executor.execute(runnable);
    }
}

