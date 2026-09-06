package com.ccadmin.app.producttraceability.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Configuration
public class ProductTraceabilityAsyncConfig {

    @Bean(name = "productTraceabilityExecutor", destroyMethod = "close")
    public ExecutorService productTraceabilityExecutor() {
        ThreadFactory threadFactory = Thread.ofVirtual()
                .name("product-traceability-", 0)
                .factory();
        return Executors.newThreadPerTaskExecutor(threadFactory);
    }
}
