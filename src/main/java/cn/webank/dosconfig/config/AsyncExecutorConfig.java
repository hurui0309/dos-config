package cn.webank.dosconfig.config;

import cn.webank.weup.biz.threadpool.WeupThreadPoolTaskExecutor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 异步线程池统一配置。
 */
@Configuration
public class AsyncExecutorConfig {

    private static final int ASYNC_CORE_SIZE = 4;
    private static final int ASYNC_MAX_SIZE = 8;
    private static final int ASYNC_QUEUE_CAPACITY = 200;
    private static final int ASYNC_KEEP_ALIVE_SECONDS = 60;

    @Bean
    @Qualifier("TPAsync")
    public WeupThreadPoolTaskExecutor weupTaskExecutor() {
        WeupThreadPoolTaskExecutor executor = new WeupThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("weup-async-");
        executor.setCorePoolSize(ASYNC_CORE_SIZE);
        executor.setMaxPoolSize(ASYNC_MAX_SIZE);
        executor.setQueueCapacity(ASYNC_QUEUE_CAPACITY);
        executor.setKeepAliveSeconds(ASYNC_KEEP_ALIVE_SECONDS);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }
}

