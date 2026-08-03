package com.ccadmin.app.shared.config;

import com.ccadmin.app.sale.service.SalePendingExpirationScheduler;
import com.ccadmin.app.shared.model.constants.BusinessConfigConstants;
import com.ccadmin.app.shared.model.dto.ConfigAutomaticProcessThreads;
import com.ccadmin.app.shared.shared.CatalogSearchShared;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.FixedDelayTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.time.Duration;

@Configuration
@Slf4j
public class ApplicationTaskSchedulerConfig implements SchedulingConfigurer {

    private static final int SCHEDULER_POOL_SIZE = 4;
    private static final Duration DEFAULT_INITIAL_DELAY = Duration.ZERO;
    private static final Duration DEFAULT_EXECUTION_CYCLE = Duration.ofMillis(3_600_000L);

    private final SalePendingExpirationScheduler salePendingExpirationScheduler;
    private final CatalogSearchShared catalogSearchShared;

    public ApplicationTaskSchedulerConfig(
            SalePendingExpirationScheduler salePendingExpirationScheduler,
            CatalogSearchShared catalogSearchShared
    ) {
        this.salePendingExpirationScheduler = salePendingExpirationScheduler;
        this.catalogSearchShared = catalogSearchShared;
    }

    @Bean
    public ThreadPoolTaskScheduler applicationTaskScheduler() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(SCHEDULER_POOL_SIZE);
        taskScheduler.setThreadNamePrefix("ccadmin-scheduler-");
        taskScheduler.setWaitForTasksToCompleteOnShutdown(true);
        taskScheduler.setAwaitTerminationSeconds(60);
        return taskScheduler;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setTaskScheduler(this.applicationTaskScheduler());
        this.registerFixedDelayTask(
                taskRegistrar,
                BusinessConfigConstants.ConfigCod.SALE_PENDING_EXPIRATION,
                this.salePendingExpirationScheduler::cancelExpiredPendingSales
        );
    }

    private void registerFixedDelayTask(
            ScheduledTaskRegistrar taskRegistrar,
            String configCod,
            Runnable process
    ) {
        ProcessSchedule schedule = this.findProcessSchedule(configCod);
        taskRegistrar.addFixedDelayTask(new FixedDelayTask(
                process,
                schedule.executionCycle(),
                schedule.initialDelay()
        ));
        log.info(
                "PROCESO_AUTOMATICO_CONFIGURADO -->> {} | DELAY: {} MS | CICLO: {} MS",
                configCod,
                schedule.initialDelay().toMillis(),
                schedule.executionCycle().toMillis()
        );
    }

    private ProcessSchedule findProcessSchedule(String configCod) {
        try {
            ConfigAutomaticProcessThreads config =
                    this.catalogSearchShared.findConfigAutomaticProcessThreads(
                            configCod
                    );
            if (config == null || config.InitialDelay == null || config.InitialDelay < 0
                    || config.ExecutionCycle == null || config.ExecutionCycle < 1) {
                log.warn(
                        "CONFIGURACION_PROCESO_AUTOMATICO_INVALIDA -->> {}. Se usaran los valores por defecto.",
                        configCod
                );
                return defaultSchedule();
            }
            return new ProcessSchedule(
                    Duration.ofMillis(config.InitialDelay),
                    Duration.ofMillis(config.ExecutionCycle)
            );
        } catch (RuntimeException exception) {
            log.error(
                    "ERROR_CONFIGURACION_PROCESO_AUTOMATICO -->> {}. Se usaran los valores por defecto: {}",
                    configCod,
                    exception.getMessage()
            );
            return defaultSchedule();
        }
    }

    private ProcessSchedule defaultSchedule() {
        return new ProcessSchedule(DEFAULT_INITIAL_DELAY, DEFAULT_EXECUTION_CYCLE);
    }

    private record ProcessSchedule(Duration initialDelay, Duration executionCycle) {
    }
}
