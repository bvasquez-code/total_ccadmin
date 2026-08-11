package com.ccadmin.app.delivery.service;

import com.ccadmin.app.delivery.model.dto.DeliveryCoverageDto;
import com.ccadmin.app.delivery.model.dto.DeliveryCoverageRequestDto;
import com.ccadmin.app.delivery.model.dto.StoreDeliveryContextDto;
import com.ccadmin.app.delivery.model.dto.StoreLocationRequestDto;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.model.entity.StoreVirtualConfigEntity;
import com.ccadmin.app.sale.repository.StoreVirtualConfigRepository;
import com.ccadmin.app.store.model.entity.StoreEntity;
import com.ccadmin.app.store.model.idto.IStoreVirtualCandidateDto;
import com.ccadmin.app.store.repository.StoreRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;

@Service
public class StoreDeliverySearchService {

    private static final double EARTH_RADIUS_KM = 6371.0088D;

    private final StoreRepository storeRepository;
    private final StoreVirtualConfigRepository storeVirtualConfigRepository;

    public StoreDeliverySearchService(
            StoreRepository storeRepository,
            StoreVirtualConfigRepository storeVirtualConfigRepository
    ) {
        this.storeRepository = storeRepository;
        this.storeVirtualConfigRepository = storeVirtualConfigRepository;
    }

    public StoreDeliveryContextDto resolveLocation(StoreLocationRequestDto request) {
        validateCoordinates(request);

        StoreCandidateDistance selected = storeRepository.findAllActiveVirtualCandidates().stream()
                .map(candidate -> new StoreCandidateDistance(
                        candidate,
                        distanceKm(
                                request.Latitude.doubleValue(),
                                request.Longitude.doubleValue(),
                                candidate.getLatitude().doubleValue(),
                                candidate.getLongitude().doubleValue()
                        )
                ))
                .filter(this::hasAvailableModality)
                .min(Comparator.comparingDouble(StoreCandidateDistance::distanceKm))
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe una tienda virtual que atienda la ubicación indicada"
                ));

        return buildContext(request, selected);
    }

    public DeliveryCoverageDto validateCoverage(DeliveryCoverageRequestDto request) {
        if (request == null || request.StoreCod == null || request.StoreCod.isBlank()
                || request.DeliveryTypeCod == null || request.DeliveryTypeCod.isBlank()) {
            throw new IllegalArgumentException("La tienda y modalidad de entrega son obligatorias");
        }

        StoreVirtualConfigEntity config = validateVirtualStore(request.StoreCod);
        StoreEntity store = storeRepository.findByStoreCod(request.StoreCod)
                .orElseThrow(() -> new IllegalArgumentException("La tienda indicada no existe"));

        DeliveryCoverageDto result = new DeliveryCoverageDto();
        result.StoreCod = request.StoreCod;
        result.DeliveryTypeCod = request.DeliveryTypeCod;

        if (SaleConstants.DELIVERY_TYPE_STORE_PICKUP.equals(request.DeliveryTypeCod)) {
            result.IsAvailable = isEnabled(config.AllowsStorePickup) ? "S" : "N";
            result.Message = "S".equals(result.IsAvailable)
                    ? "Recojo disponible en " + store.Name
                    : "Esta tienda no tiene habilitado el recojo en tienda";
            return result;
        }

        validateCoordinates(request.Latitude, request.Longitude);
        result.DistanceKm = calculateDistance(store, request.Latitude, request.Longitude);

        if (SaleConstants.DELIVERY_TYPE_AUTOMATIC.equals(request.DeliveryTypeCod)) {
            result.MaximumDistanceKm = config.AutomaticDeliveryRadiusKm;
            result.IsAvailable = enabledWithinRadius(
                    config.AllowsAutomaticDelivery,
                    config.AutomaticDeliveryRadiusKm,
                    result.DistanceKm.doubleValue()
            ) ? "S" : "N";
            result.Message = isEnabled(config.AllowsAutomaticDelivery)
                    ? buildCoverageMessage(
                            result,
                            "Delivery disponible para esta dirección",
                            "Esta dirección está fuera del radio de delivery"
                    )
                    : "La tienda no tiene habilitado el delivery automático";
            return result;
        }

        if (SaleConstants.DELIVERY_TYPE_SCHEDULED.equals(request.DeliveryTypeCod)) {
            result.MaximumDistanceKm = config.ScheduledDeliveryMaxRadiusKm;
            result.IsAvailable = enabledWithinRadius(
                    config.AllowsScheduledDelivery,
                    config.ScheduledDeliveryMaxRadiusKm,
                    result.DistanceKm.doubleValue()
            ) ? "S" : "N";
            result.Message = isEnabled(config.AllowsScheduledDelivery)
                    ? buildCoverageMessage(
                            result,
                            "Entrega programada disponible para esta dirección",
                            "Esta dirección está fuera del radio de entrega programada"
                    )
                    : "La tienda no tiene habilitada la entrega programada";
            return result;
        }

        throw new IllegalArgumentException("La modalidad de entrega indicada no es válida");
    }

    public StoreVirtualConfigEntity validateVirtualStore(String storeCod) {
        if (storeCod == null || storeCod.isBlank()) {
            throw new IllegalArgumentException("La tienda es obligatoria");
        }
        return storeVirtualConfigRepository.findActiveByStoreCod(storeCod)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La tienda indicada no está habilitada para la tienda virtual"
                ));
    }

    public BigDecimal calculateDistance(
            StoreEntity store,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        if (store.Latitude == null || store.Longitude == null || latitude == null || longitude == null) {
            throw new IllegalArgumentException("La tienda y la entrega deben tener coordenadas válidas");
        }
        return BigDecimal.valueOf(distanceKm(
                latitude.doubleValue(),
                longitude.doubleValue(),
                store.Latitude.doubleValue(),
                store.Longitude.doubleValue()
        )).setScale(3, RoundingMode.HALF_UP);
    }

    public StoreEntity findActiveVirtualStore(String storeCod) {
        validateVirtualStore(storeCod);
        return storeRepository.findByStoreCod(storeCod)
                .orElseThrow(() -> new IllegalArgumentException("La tienda indicada no existe"));
    }

    private StoreDeliveryContextDto buildContext(
            StoreLocationRequestDto request,
            StoreCandidateDistance selected
    ) {
        IStoreVirtualCandidateDto candidate = selected.candidate();
        StoreDeliveryContextDto context = new StoreDeliveryContextDto();
        context.Store = toStore(candidate);
        context.Latitude = request.Latitude;
        context.Longitude = request.Longitude;
        context.Address = request.Address;
        context.DistanceKm = BigDecimal.valueOf(selected.distanceKm()).setScale(3, RoundingMode.HALF_UP);
        context.AllowsAutomaticDelivery = enabledWithinRadius(
                candidate.getAllowsAutomaticDelivery(),
                candidate.getAutomaticDeliveryRadiusKm(),
                selected.distanceKm()
        ) ? "S" : "N";
        context.AllowsScheduledDelivery = enabledWithinRadius(
                candidate.getAllowsScheduledDelivery(),
                candidate.getScheduledDeliveryMaxRadiusKm(),
                selected.distanceKm()
        ) ? "S" : "N";
        context.AllowsStorePickup = isEnabled(candidate.getAllowsStorePickup()) ? "S" : "N";
        context.DeliveryMessage = buildDeliveryMessage(context);
        return context;
    }

    private StoreEntity toStore(IStoreVirtualCandidateDto source) {
        StoreEntity store = new StoreEntity();
        store.StoreCod = source.getStoreCod();
        store.Name = source.getName();
        store.Description = source.getDescription();
        store.Address = source.getAddress();
        store.UbigeoCod = source.getUbigeoCod();
        store.IsVirtualStoreEnabled = "S";
        store.Latitude = source.getLatitude();
        store.Longitude = source.getLongitude();
        return store;
    }

    private boolean hasAvailableModality(StoreCandidateDistance item) {
        IStoreVirtualCandidateDto candidate = item.candidate();
        return enabledWithinRadius(
                candidate.getAllowsAutomaticDelivery(),
                candidate.getAutomaticDeliveryRadiusKm(),
                item.distanceKm()
        ) || enabledWithinRadius(
                candidate.getAllowsScheduledDelivery(),
                candidate.getScheduledDeliveryMaxRadiusKm(),
                item.distanceKm()
        ) || isEnabled(candidate.getAllowsStorePickup());
    }

    private boolean enabledWithinRadius(String indicator, BigDecimal radius, double distance) {
        return isEnabled(indicator) && radius != null && distance <= radius.doubleValue();
    }

    private boolean isEnabled(String indicator) {
        return "S".equalsIgnoreCase(indicator);
    }

    private String buildDeliveryMessage(StoreDeliveryContextDto context) {
        if ("S".equals(context.AllowsAutomaticDelivery)) {
            return "Delivery disponible desde " + context.Store.Name;
        }
        if ("S".equals(context.AllowsScheduledDelivery)) {
            return "La ubicación será atendida mediante una entrega programada";
        }
        return "La compra puede recogerse en " + context.Store.Name;
    }

    private String buildCoverageMessage(
            DeliveryCoverageDto coverage,
            String availableMessage,
            String unavailableMessage
    ) {
        String distanceMessage = " (distancia aproximada: "
                + coverage.DistanceKm.stripTrailingZeros().toPlainString() + " km)";
        if ("S".equals(coverage.IsAvailable)) {
            return availableMessage + distanceMessage;
        }
        if (coverage.MaximumDistanceKm == null) {
            return unavailableMessage + ": la modalidad no está habilitada en la tienda";
        }
        return unavailableMessage + ". Máximo permitido: "
                + coverage.MaximumDistanceKm.stripTrailingZeros().toPlainString()
                + " km" + distanceMessage;
    }

    private void validateCoordinates(StoreLocationRequestDto request) {
        if (request == null || request.Latitude == null || request.Longitude == null) {
            throw new IllegalArgumentException("La latitud y longitud son obligatorias");
        }
        validateCoordinates(request.Latitude, request.Longitude);
    }

    private void validateCoordinates(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("La latitud y longitud son obligatorias");
        }
        if (latitude.compareTo(BigDecimal.valueOf(-90)) < 0
                || latitude.compareTo(BigDecimal.valueOf(90)) > 0
                || longitude.compareTo(BigDecimal.valueOf(-180)) < 0
                || longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new IllegalArgumentException("Las coordenadas indicadas no son válidas");
        }
    }

    private double distanceKm(double latitudeA, double longitudeA, double latitudeB, double longitudeB) {
        double latitudeDistance = Math.toRadians(latitudeB - latitudeA);
        double longitudeDistance = Math.toRadians(longitudeB - longitudeA);
        double value = Math.sin(latitudeDistance / 2) * Math.sin(latitudeDistance / 2)
                + Math.cos(Math.toRadians(latitudeA)) * Math.cos(Math.toRadians(latitudeB))
                * Math.sin(longitudeDistance / 2) * Math.sin(longitudeDistance / 2);
        value = Math.min(1D, Math.max(0D, value));
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(value), Math.sqrt(1 - value));
    }

    private record StoreCandidateDistance(IStoreVirtualCandidateDto candidate, double distanceKm) {
    }
}
