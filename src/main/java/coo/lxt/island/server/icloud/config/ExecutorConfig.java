package coo.lxt.island.server.icloud.config;

import coo.lxt.island.server.icloud.constant.ExecutorConstant;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ExecutorConfig {

    private static final int CORE_POOL_SIZE_FOR_IO_TASKS;
    private static final int CORE_POOL_SIZE_FOR_CPU_TASKS;

    static {
        int cores = Runtime.getRuntime().availableProcessors();
        CORE_POOL_SIZE_FOR_IO_TASKS = cores * 2;
        CORE_POOL_SIZE_FOR_CPU_TASKS = cores + 1;
    }

    @Bean(name = ExecutorConstant.ICLOUD_SESSION_MANAGER)
    public AsyncTaskExecutor icloudSessionManagerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE_FOR_IO_TASKS);
        executor.setMaxPoolSize(CORE_POOL_SIZE_FOR_IO_TASKS);
        executor.setQueueCapacity(CORE_POOL_SIZE_FOR_IO_TASKS * 20);
        executor.setThreadNamePrefix(ExecutorConstant.ICLOUD_SESSION_MANAGER);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setKeepAliveSeconds(60);
        executor.initialize();
        return executor;
    }

    @Bean(name = ExecutorConstant.ICLOUD_WEB_SERVICE_MANAGER)
    public AsyncTaskExecutor icloudWebServiceManagerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE_FOR_IO_TASKS);
        executor.setMaxPoolSize(CORE_POOL_SIZE_FOR_IO_TASKS);
        executor.setQueueCapacity(CORE_POOL_SIZE_FOR_IO_TASKS * 20);
        executor.setThreadNamePrefix(ExecutorConstant.ICLOUD_SESSION_MANAGER);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setKeepAliveSeconds(60);
        executor.initialize();
        return executor;
    }
}
