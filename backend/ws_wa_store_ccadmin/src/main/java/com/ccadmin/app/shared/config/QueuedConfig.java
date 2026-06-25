package com.ccadmin.app.shared.config;

import com.ccadmin.app.shared.service.IGenericTaskService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Configuration
public class QueuedConfig {

    @Bean
    public BlockingQueue<IGenericTaskService> queueTask() {
        return new LinkedBlockingQueue<>();
    }

}
