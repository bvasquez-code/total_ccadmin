package com.ccadmin.app.shared.config;

import com.ccadmin.app.sale.service.SalePendingExpirationScheduler;
import com.ccadmin.app.shared.model.constants.BusinessConfigConstants;
import com.ccadmin.app.shared.model.dto.ConfigAutomaticProcessThreads;
import com.ccadmin.app.shared.shared.CatalogSearchShared;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.config.IntervalTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApplicationTaskSchedulerConfigTest {

    @Test
    void registersPendingSaleProcessUsingCatalogConfiguration() {
        SalePendingExpirationScheduler pendingSaleScheduler =
                mock(SalePendingExpirationScheduler.class);
        CatalogSearchShared catalogSearchShared = mock(CatalogSearchShared.class);
        when(catalogSearchShared.findConfigAutomaticProcessThreads(
                BusinessConfigConstants.ConfigCod.SALE_PENDING_EXPIRATION
        )).thenReturn(new ConfigAutomaticProcessThreads(
                BusinessConfigConstants.ConfigCod.SALE_PENDING_EXPIRATION,
                "Cancelacion de ventas pendientes",
                5_000L,
                90_000L,
                1,
                1
        ));
        ApplicationTaskSchedulerConfig config =
                new ApplicationTaskSchedulerConfig(pendingSaleScheduler, catalogSearchShared);
        ScheduledTaskRegistrar taskRegistrar = new ScheduledTaskRegistrar();

        config.configureTasks(taskRegistrar);

        assertNotNull(taskRegistrar.getScheduler());
        assertEquals(1, taskRegistrar.getFixedDelayTaskList().size());
        IntervalTask registeredTask = taskRegistrar.getFixedDelayTaskList().get(0);
        assertEquals(Duration.ofMillis(5_000L), registeredTask.getInitialDelayDuration());
        assertEquals(Duration.ofMillis(90_000L), registeredTask.getIntervalDuration());
        verify(catalogSearchShared).findConfigAutomaticProcessThreads(
                BusinessConfigConstants.ConfigCod.SALE_PENDING_EXPIRATION
        );

        registeredTask.getRunnable().run();
        verify(pendingSaleScheduler).cancelExpiredPendingSales();
    }

    @Test
    void usesSafeDefaultsWhenCatalogConfigurationIsInvalid() {
        SalePendingExpirationScheduler pendingSaleScheduler =
                mock(SalePendingExpirationScheduler.class);
        CatalogSearchShared catalogSearchShared = mock(CatalogSearchShared.class);
        when(catalogSearchShared.findConfigAutomaticProcessThreads(
                BusinessConfigConstants.ConfigCod.SALE_PENDING_EXPIRATION
        )).thenReturn(new ConfigAutomaticProcessThreads(
                BusinessConfigConstants.ConfigCod.SALE_PENDING_EXPIRATION,
                "Configuracion invalida",
                -1L,
                0L,
                1,
                1
        ));
        ApplicationTaskSchedulerConfig config =
                new ApplicationTaskSchedulerConfig(pendingSaleScheduler, catalogSearchShared);
        ScheduledTaskRegistrar taskRegistrar = new ScheduledTaskRegistrar();

        config.configureTasks(taskRegistrar);

        IntervalTask registeredTask = taskRegistrar.getFixedDelayTaskList().get(0);
        assertEquals(Duration.ZERO, registeredTask.getInitialDelayDuration());
        assertEquals(Duration.ofMillis(3_600_000L), registeredTask.getIntervalDuration());
    }
}
