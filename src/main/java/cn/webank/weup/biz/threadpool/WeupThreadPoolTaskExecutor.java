package cn.webank.weup.biz.threadpool;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 迪奥线程池占位实现，继承 Spring ThreadPoolTaskExecutor。
 */
public class WeupThreadPoolTaskExecutor extends ThreadPoolTaskExecutor {

    @Override
    public void execute(Runnable task) {
        super.execute(task);
    }
}

