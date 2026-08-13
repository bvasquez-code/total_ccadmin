package com.ccadmin.app.delivery.service;

import com.ccadmin.app.delivery.model.dto.DeliveryCoverageDto;
import com.ccadmin.app.delivery.model.dto.DeliveryCoverageRequestDto;
import com.ccadmin.app.delivery.model.dto.ShippingPriceDto;
import com.ccadmin.app.delivery.model.dto.ShippingPriceRequestDto;
import com.ccadmin.app.delivery.model.dto.ShippingScheduleDto;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.shared.model.constants.BusinessConfigConstants;
import com.ccadmin.app.shared.model.entity.BusinessConfigEntity;
import com.ccadmin.app.shared.service.BusinessConfigSearchService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class ShippingPriceSearchService {

    private static final String COUNTRY_PERU = "PER";

    private final StoreDeliverySearchService storeDeliverySearchService;
    private final ShippingScheduleSearchService shippingScheduleSearchService;
    private final BusinessConfigSearchService businessConfigSearchService;

    public ShippingPriceSearchService(
            StoreDeliverySearchService storeDeliverySearchService,
            ShippingScheduleSearchService shippingScheduleSearchService,
            BusinessConfigSearchService businessConfigSearchService
    ) {
        this.storeDeliverySearchService = storeDeliverySearchService;
        this.shippingScheduleSearchService = shippingScheduleSearchService;
        this.businessConfigSearchService = businessConfigSearchService;
    }

    public ShippingPriceDto findPrice(ShippingPriceRequestDto request) {
        validateRequest(request);
        DeliveryCoverageDto coverage = storeDeliverySearchService.validateCoverage(
                buildCoverageRequest(request)
        );
        if (!"S".equals(coverage.IsAvailable)) {
            throw new IllegalArgumentException(coverage.Message);
        }

        ShippingScheduleDto schedule = null;
        String shippingConfigCod = BusinessConfigConstants.ConfigCod.SHIPPING_LOCAL;
        if (SaleConstants.DELIVERY_TYPE_SCHEDULED.equals(request.DeliveryTypeCod)) {
            schedule = shippingScheduleSearchService.findSchedule(request, coverage);
            shippingConfigCod = schedule.ShippingConfigCod;
        }

        BusinessConfigEntity priceConfig = findActivePriceConfiguration(shippingConfigCod);
        BigDecimal priceBase = BigDecimal.valueOf(priceConfig.Num1Config);
        BigDecimal pricePerKm = BigDecimal.valueOf(priceConfig.Num2Config);
        BigDecimal amount = priceBase.add(coverage.DistanceKm.multiply(pricePerKm))
                .setScale(0, RoundingMode.HALF_UP);

        ShippingPriceDto result = new ShippingPriceDto();
        result.DeliveryTypeCod = request.DeliveryTypeCod;
        result.ScheduleType = schedule == null ? null : schedule.ScheduleType;
        result.ShippingConfigCod = shippingConfigCod;
        result.ProductCod = priceConfig.ConfigName;
        result.Description = priceConfig.ConfigDesc;
        result.DistanceKm = coverage.DistanceKm;
        result.PriceBase = priceBase.setScale(2, RoundingMode.HALF_UP);
        result.PricePerKm = pricePerKm.setScale(2, RoundingMode.HALF_UP);
        result.Amount = amount;
        result.Coverage = coverage;
        result.Schedule = schedule;
        return result;
    }

    private BusinessConfigEntity findActivePriceConfiguration(String shippingConfigCod) {
        if (shippingConfigCod == null || shippingConfigCod.isBlank()) {
            throw new IllegalArgumentException("No se configuró la tarifa para esta modalidad de entrega");
        }
        BusinessConfigEntity result = businessConfigSearchService.findActivesByGroupCod(
                        BusinessConfigConstants.GroupCod.SHIPPING_CONFIG
                ).stream()
                .filter(item -> shippingConfigCod.equals(item.ConfigCod))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe una tarifa de envío activa para " + shippingConfigCod
                ));
        if (result.ConfigName == null || result.ConfigName.isBlank()
                || result.Num1Config == null || result.Num1Config < 0
                || result.Num2Config == null || result.Num2Config < 0) {
            throw new IllegalArgumentException(
                    "La tarifa de envío " + shippingConfigCod + " no está configurada correctamente"
            );
        }
        return result;
    }

    private DeliveryCoverageRequestDto buildCoverageRequest(ShippingPriceRequestDto request) {
        DeliveryCoverageRequestDto coverageRequest = new DeliveryCoverageRequestDto();
        coverageRequest.StoreCod = request.StoreCod;
        coverageRequest.DeliveryTypeCod = request.DeliveryTypeCod;
        coverageRequest.Latitude = request.Latitude;
        coverageRequest.Longitude = request.Longitude;
        return coverageRequest;
    }

    private void validateRequest(ShippingPriceRequestDto request) {
        if (request == null || request.StoreCod == null || request.StoreCod.isBlank()) {
            throw new IllegalArgumentException("La tienda es obligatoria");
        }
        if (!SaleConstants.DELIVERY_TYPE_AUTOMATIC.equals(request.DeliveryTypeCod)
                && !SaleConstants.DELIVERY_TYPE_SCHEDULED.equals(request.DeliveryTypeCod)) {
            throw new IllegalArgumentException("La modalidad indicada no genera costo de envío");
        }
        if (request.Latitude == null || request.Longitude == null) {
            throw new IllegalArgumentException("La dirección debe tener coordenadas válidas");
        }
        if (request.CountryCod != null && !request.CountryCod.isBlank()
                && !COUNTRY_PERU.equalsIgnoreCase(request.CountryCod.trim())) {
            throw new IllegalArgumentException("Los envíos internacionales todavía no están disponibles");
        }
    }
}
