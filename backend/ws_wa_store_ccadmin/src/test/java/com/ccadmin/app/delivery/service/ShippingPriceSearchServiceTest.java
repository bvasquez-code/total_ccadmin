package com.ccadmin.app.delivery.service;

import com.ccadmin.app.delivery.model.dto.DeliveryCoverageDto;
import com.ccadmin.app.delivery.model.dto.ShippingPriceDto;
import com.ccadmin.app.delivery.model.dto.ShippingPriceRequestDto;
import com.ccadmin.app.delivery.model.dto.ShippingScheduleDto;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.shared.model.entity.BusinessConfigEntity;
import com.ccadmin.app.shared.service.BusinessConfigSearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShippingPriceSearchServiceTest {

    @Mock private StoreDeliverySearchService storeDeliverySearchService;
    @Mock private ShippingScheduleSearchService shippingScheduleSearchService;
    @Mock private BusinessConfigSearchService businessConfigSearchService;

    @Test
    void calculatesLocalPriceUsingExactDistanceAndRoundsTheFinalAmount() {
        ShippingPriceSearchService service = service();
        DeliveryCoverageDto coverage = coverage("15.727");
        when(storeDeliverySearchService.validateCoverage(any())).thenReturn(coverage);
        when(businessConfigSearchService.findActivesByGroupCod("ShippingConfig"))
                .thenReturn(List.of(priceConfig("ShippingLocal", 5, 1)));

        ShippingPriceDto result = service.findPrice(request(SaleConstants.DELIVERY_TYPE_AUTOMATIC));

        assertEquals("ShippingLocal", result.ShippingConfigCod);
        assertEquals(SaleConstants.SHIPPING_PRODUCT_COD, result.ProductCod);
        assertEquals(new BigDecimal("21"), result.Amount);
    }

    @Test
    void usesTheShippingConfigurationRelatedToTheScheduledClassification() {
        ShippingPriceSearchService service = service();
        DeliveryCoverageDto coverage = coverage("15.727");
        when(storeDeliverySearchService.validateCoverage(any())).thenReturn(coverage);
        ShippingScheduleDto schedule = new ShippingScheduleDto();
        schedule.ScheduleType = "ScheduledSameDepartment";
        schedule.ShippingConfigCod = "ShippingNational";
        when(shippingScheduleSearchService.findSchedule(any(), any())).thenReturn(schedule);
        when(businessConfigSearchService.findActivesByGroupCod("ShippingConfig"))
                .thenReturn(List.of(priceConfig("ShippingNational", 25, 0)));

        ShippingPriceDto result = service.findPrice(request(SaleConstants.DELIVERY_TYPE_SCHEDULED));

        assertEquals("ScheduledSameDepartment", result.ScheduleType);
        assertEquals("ShippingNational", result.ShippingConfigCod);
        assertEquals(new BigDecimal("25"), result.Amount);
        assertEquals(schedule, result.Schedule);
    }

    private ShippingPriceSearchService service() {
        return new ShippingPriceSearchService(
                storeDeliverySearchService,
                shippingScheduleSearchService,
                businessConfigSearchService
        );
    }

    private ShippingPriceRequestDto request(String deliveryTypeCod) {
        ShippingPriceRequestDto request = new ShippingPriceRequestDto();
        request.StoreCod = "T001";
        request.DeliveryTypeCod = deliveryTypeCod;
        request.Latitude = new BigDecimal("-6.78");
        request.Longitude = new BigDecimal("-79.84");
        request.CountryCod = "PER";
        request.UbigeoCod = "140101";
        return request;
    }

    private DeliveryCoverageDto coverage(String distanceKm) {
        DeliveryCoverageDto coverage = new DeliveryCoverageDto();
        coverage.IsAvailable = "S";
        coverage.DistanceKm = new BigDecimal(distanceKm);
        return coverage;
    }

    private BusinessConfigEntity priceConfig(
            String configCod,
            int priceBase,
            int pricePerKm
    ) {
        BusinessConfigEntity config = new BusinessConfigEntity();
        config.ConfigCod = configCod;
        config.ConfigName = SaleConstants.SHIPPING_PRODUCT_COD;
        config.ConfigDesc = "Costo de envío";
        config.Num1Config = priceBase;
        config.Num2Config = pricePerKm;
        return config;
    }
}
