package com.compucafe.executordemo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AppConfig {
    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);
    @Value("${com.compucafe.executor-demo.corePoolSize:10}")
    int corePoolSize;
    @Value("${com.compucafe.executor-demo.maxPoolSize:50}")
    int maxPoolSize;
    @Value("${com.compucafe.executor-service.queueCapacity:20}")
    int queueCapacity;
    @Value("${com.compucafe.executor-demo.keepAliveSeconds:30}")
    int keepAliveSeconds;
    @Value("${com.compucafe.executor-demo.threadNamePrefix:tpExec-}")
    String threadNamePrefix;
    @Value("${com.compucafe.executor-demo.allowCoreThreadTimeout:true}")
    Boolean allowCoreThreadTimeout;

    @Bean
    ThreadPoolTaskExecutor threadPoolTaskExecutor() {
        log.info(String.format("Configuring ThreadPoolTaskExecutor with corePoolSize: [%d], maxPoolSize: [%d], queueCapacity: [%d] keepAliveSeconds: [%d], threadNamePrefix: [%s]",
                corePoolSize, maxPoolSize, queueCapacity, keepAliveSeconds, threadNamePrefix));
        ThreadPoolTaskExecutor retBean = new ThreadPoolTaskExecutor();
        retBean.setCorePoolSize(corePoolSize);
        retBean.setMaxPoolSize(maxPoolSize);
        retBean.setQueueCapacity(queueCapacity);
        retBean.setKeepAliveSeconds(keepAliveSeconds);
        retBean.setThreadNamePrefix(threadNamePrefix);
        retBean.setAllowCoreThreadTimeOut(allowCoreThreadTimeout);

        return retBean;
    }
}
