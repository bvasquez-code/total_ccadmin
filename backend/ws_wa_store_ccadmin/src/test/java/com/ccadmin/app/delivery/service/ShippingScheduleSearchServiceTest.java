package com.ccadmin.app.delivery.service;

import com.ccadmin.app.delivery.model.dto.DeliveryCoverageDto;
import com.ccadmin.app.delivery.model.dto.ShippingScheduleDto;
import com.ccadmin.app.delivery.model.dto.ShippingScheduleRequestDto;
import com.ccadmin.app.sale.model.entity.StoreVirtualConfigEntity;
import com.ccadmin.app.shared.model.entity.BusinessConfigEntity;
import com.ccadmin.app.shared.repository.UbigeoRepository;
import com.ccadmin.app.shared.service.BusinessConfigSearchService;
import com.ccadmin.app.store.model.entity.StoreEntity;
import com.ccadmin.app.store.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShippingScheduleSearchServiceTest {

    @Mock private StoreDeliverySearchService storeDeliverySearchService;
    @Mock private StoreRepository storeRepository;
    @Mock private BusinessConfigSearchService businessConfigSearchService;
    @Mock private UbigeoRepository ubigeoRepository;

    private ShippingScheduleSearchService service;
    private StoreVirtualConfigEntity storeVirtualConfig;
    private StoreEntity store;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-13T14:00:00Z"),
                ZoneId.of("America/Lima")
        );
        service = new ShippingScheduleSearchService(
                storeDeliverySearchService,
                storeRepository,
                businessConfigSearchService,
                ubigeoRepository,
                clock
        );

        storeVirtualConfig = new StoreVirtualConfigEntity();
        storeVirtualConfig.AutomaticDeliveryRadiusKm = new BigDecimal("10");
        storeVirtualConfig.ScheduledDeliveryMaxRadiusKm = new BigDecimal("100");
        storeVirtualConfig.PreparationTimeMinutes = 0;
        store = new StoreEntity();
        store.StoreCod = "T001";
        store.UbigeoCod = "140101";

        when(storeDeliverySearchService.validateVirtualStore("T001"))
                .thenReturn(storeVirtualConfig);
        when(storeRepository.findByStoreCod("T001")).thenReturn(Optional.of(store));
    }

    @Test
    void generatesTodayTomorrowAndConfiguredTimeSlotsForLocalDelivery() {
        coverageDistance("3.5");
        configure(scheduleConfig(
                ShippingScheduleSearchService.SCHEDULED_LOCAL,
                0,
                1,
                "10:00",
                "18:00",
                2,
                "S"
        ));

        ShippingScheduleDto result = service.findSchedule(request("140101"));

        assertEquals(ShippingScheduleSearchService.SCHEDULED_LOCAL, result.ScheduleType);
        assertEquals(2, result.DateList.size());
        assertEquals("Hoy - 13/08/2026", result.DateList.get(0).Label);
        assertEquals("Mañana - 14/08/2026", result.DateList.get(1).Label);
        assertEquals(4, result.DateList.get(0).TimeSlotList.size());
        assertEquals("10:00 - 12:00", result.DateList.get(0).TimeSlotList.get(0).Label);
        assertEquals("16:00 - 18:00", result.DateList.get(0).TimeSlotList.get(3).Label);
    }

    @Test
    void usesSameDepartmentAndDirectDayOffsetsWhenAutomaticRadiusIsExceeded() {
        coverageDistance("15.727");
        when(ubigeoRepository.findDepartmentCodByUbigeoCod("140101"))
                .thenReturn(Optional.of("14"));
        when(ubigeoRepository.findDepartmentCodByUbigeoCod("140102"))
                .thenReturn(Optional.of("14"));
        configure(scheduleConfig(
                ShippingScheduleSearchService.SCHEDULED_SAME_DEPARTMENT,
                1,
                2,
                "10:00",
                "18:00",
                2,
                "S"
        ));

        ShippingScheduleDto result = service.findSchedule(request("140102"));

        assertEquals(ShippingScheduleSearchService.SCHEDULED_SAME_DEPARTMENT, result.ScheduleType);
        assertEquals("Mañana - 14/08/2026", result.DateList.get(0).Label);
        assertEquals("15/08/2026", result.DateList.get(1).Label);
        assertEquals("2026-08-14", result.DateList.get(0).Date);
        assertEquals("2026-08-15", result.DateList.get(1).Date);
    }

    @Test
    void generatesOnlyConfiguredDatesForDifferentDepartment() {
        coverageDistance("20");
        when(ubigeoRepository.findDepartmentCodByUbigeoCod("140101"))
                .thenReturn(Optional.of("14"));
        when(ubigeoRepository.findDepartmentCodByUbigeoCod("150101"))
                .thenReturn(Optional.of("15"));
        configure(scheduleConfig(
                ShippingScheduleSearchService.SCHEDULED_DEPARTMENTAL,
                5,
                10,
                null,
                null,
                null,
                "N"
        ));

        ShippingScheduleDto result = service.findSchedule(request("150101"));

        assertEquals(ShippingScheduleSearchService.SCHEDULED_DEPARTMENTAL, result.ScheduleType);
        assertEquals(6, result.DateList.size());
        assertEquals("2026-08-18", result.DateList.get(0).Date);
        assertEquals("2026-08-23", result.DateList.get(5).Date);
        assertEquals(0, result.DateList.get(0).TimeSlotList.size());
    }

    @Test
    void usesUnmappedConfigurationForAnAddressWithoutPeruvianUbigeo() {
        coverageDistance("20");
        configure(scheduleConfig(
                ShippingScheduleSearchService.SCHEDULED_UNMAPPED,
                5,
                10,
                null,
                null,
                null,
                "N"
        ));
        ShippingScheduleRequestDto request = request("");
        request.CountryCod = "USA";

        ShippingScheduleDto result = service.findSchedule(request);

        assertEquals(ShippingScheduleSearchService.SCHEDULED_UNMAPPED, result.ScheduleType);
        assertEquals(6, result.DateList.size());
        assertEquals("2026-08-18", result.DateList.get(0).Date);
    }

    private void coverageDistance(String distance) {
        DeliveryCoverageDto coverage = new DeliveryCoverageDto();
        coverage.IsAvailable = "S";
        coverage.DistanceKm = new BigDecimal(distance);
        when(storeDeliverySearchService.validateCoverage(any())).thenReturn(coverage);
    }

    private void configure(BusinessConfigEntity config) {
        when(businessConfigSearchService.findActivesByGroupCod("ShippingScheduleConfig"))
                .thenReturn(List.of(config));
    }

    private ShippingScheduleRequestDto request(String ubigeoCod) {
        ShippingScheduleRequestDto request = new ShippingScheduleRequestDto();
        request.StoreCod = "T001";
        request.Latitude = new BigDecimal("-6.78");
        request.Longitude = new BigDecimal("-79.84");
        request.CountryCod = "PER";
        request.UbigeoCod = ubigeoCod;
        return request;
    }

    private BusinessConfigEntity scheduleConfig(
            String type,
            int minDays,
            int maxDays,
            String startTime,
            String endTime,
            Integer slotHours,
            String useTimeSlot
    ) {
        BusinessConfigEntity config = new BusinessConfigEntity();
        config.ConfigCod = type;
        config.ConfigName = type;
        config.Num1Config = minDays;
        config.Num2Config = maxDays;
        config.Str1Config = startTime;
        config.Str2Config = endTime;
        config.Num3Config = slotHours;
        config.Sta1Config = useTimeSlot;
        return config;
    }
}
