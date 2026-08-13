package com.ccadmin.app.delivery.service;

import com.ccadmin.app.delivery.model.dto.CheckoutDeliveryDto;
import com.ccadmin.app.delivery.model.dto.DeliveryCoverageDto;
import com.ccadmin.app.delivery.model.dto.DeliveryCoverageRequestDto;
import com.ccadmin.app.delivery.model.dto.ShippingScheduleDateDto;
import com.ccadmin.app.delivery.model.dto.ShippingScheduleDto;
import com.ccadmin.app.delivery.model.dto.ShippingScheduleRequestDto;
import com.ccadmin.app.delivery.model.dto.ShippingScheduleTimeSlotDto;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.model.entity.StoreVirtualConfigEntity;
import com.ccadmin.app.shared.model.constants.BusinessConfigConstants;
import com.ccadmin.app.shared.model.entity.BusinessConfigEntity;
import com.ccadmin.app.shared.repository.UbigeoRepository;
import com.ccadmin.app.shared.service.BusinessConfigSearchService;
import com.ccadmin.app.store.model.entity.StoreEntity;
import com.ccadmin.app.store.repository.StoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@Service
public class ShippingScheduleSearchService {

    static final String SCHEDULED_LOCAL = "ScheduledLocal";
    static final String SCHEDULED_SAME_DEPARTMENT = "ScheduledSameDepartment";
    static final String SCHEDULED_DEPARTMENTAL = "ScheduledDepartmental";
    static final String SCHEDULED_UNMAPPED = "ScheduledUnmapped";

    private static final String COUNTRY_PERU = "PER";
    private static final DateTimeFormatter DATE_LABEL_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final StoreDeliverySearchService storeDeliverySearchService;
    private final StoreRepository storeRepository;
    private final BusinessConfigSearchService businessConfigSearchService;
    private final UbigeoRepository ubigeoRepository;
    private final Clock clock;

    @Autowired
    public ShippingScheduleSearchService(
            StoreDeliverySearchService storeDeliverySearchService,
            StoreRepository storeRepository,
            BusinessConfigSearchService businessConfigSearchService,
            UbigeoRepository ubigeoRepository
    ) {
        this(
                storeDeliverySearchService,
                storeRepository,
                businessConfigSearchService,
                ubigeoRepository,
                Clock.systemDefaultZone()
        );
    }

    ShippingScheduleSearchService(
            StoreDeliverySearchService storeDeliverySearchService,
            StoreRepository storeRepository,
            BusinessConfigSearchService businessConfigSearchService,
            UbigeoRepository ubigeoRepository,
            Clock clock
    ) {
        this.storeDeliverySearchService = storeDeliverySearchService;
        this.storeRepository = storeRepository;
        this.businessConfigSearchService = businessConfigSearchService;
        this.ubigeoRepository = ubigeoRepository;
        this.clock = clock;
    }

    public ShippingScheduleDto findSchedule(ShippingScheduleRequestDto request) {
        validateRequest(request);

        DeliveryCoverageDto coverage = storeDeliverySearchService.validateCoverage(
                buildCoverageRequest(request)
        );
        if (!"S".equals(coverage.IsAvailable)) {
            throw new IllegalArgumentException(coverage.Message);
        }

        StoreVirtualConfigEntity storeVirtualConfig = storeDeliverySearchService.validateVirtualStore(
                request.StoreCod
        );
        StoreEntity store = storeRepository.findByStoreCod(request.StoreCod)
                .orElseThrow(() -> new IllegalArgumentException("La tienda indicada no existe"));
        String scheduleType = determineScheduleType(
                request,
                coverage,
                storeVirtualConfig,
                store
        );
        BusinessConfigEntity config = findActiveConfiguration(scheduleType);
        return buildSchedule(config, storeVirtualConfig.PreparationTimeMinutes);
    }

    public void validateSelection(String storeCod, CheckoutDeliveryDto delivery) {
        ShippingScheduleRequestDto request = new ShippingScheduleRequestDto();
        request.StoreCod = storeCod;
        request.Latitude = delivery.Latitude;
        request.Longitude = delivery.Longitude;
        request.CountryCod = delivery.CountryCod;
        request.UbigeoCod = delivery.UbigeoCod;

        Instant scheduledFrom = parseInstant(delivery.ScheduledFrom, "inicio");
        Instant scheduledTo = parseInstant(delivery.ScheduledTo, "fin");
        if (scheduledTo.isBefore(scheduledFrom)) {
            throw new IllegalArgumentException(
                    "El final de la entrega programada no puede ser anterior al inicio"
            );
        }

        ShippingScheduleDto schedule = findSchedule(request);
        boolean validSelection = schedule.DateList.stream().anyMatch(date ->
                matches(date.ScheduledFrom, date.ScheduledTo, scheduledFrom, scheduledTo)
                        || date.TimeSlotList.stream().anyMatch(slot ->
                        matches(slot.ScheduledFrom, slot.ScheduledTo, scheduledFrom, scheduledTo)
                )
        );
        if (!validSelection) {
            throw new IllegalArgumentException(
                    "La fecha o franja horaria seleccionada ya no se encuentra disponible"
            );
        }
    }

    private String determineScheduleType(
            ShippingScheduleRequestDto request,
            DeliveryCoverageDto coverage,
            StoreVirtualConfigEntity storeVirtualConfig,
            StoreEntity store
    ) {
        if (storeVirtualConfig.AutomaticDeliveryRadiusKm != null
                && coverage.DistanceKm != null
                && coverage.DistanceKm.compareTo(storeVirtualConfig.AutomaticDeliveryRadiusKm) <= 0) {
            return SCHEDULED_LOCAL;
        }

        if (request.CountryCod != null && !request.CountryCod.isBlank()
                && !COUNTRY_PERU.equalsIgnoreCase(request.CountryCod.trim())) {
            return SCHEDULED_UNMAPPED;
        }

        Optional<String> storeDepartment = findDepartment(store.UbigeoCod);
        Optional<String> deliveryDepartment = findDepartment(request.UbigeoCod);
        if (storeDepartment.isEmpty() || deliveryDepartment.isEmpty()) {
            return SCHEDULED_UNMAPPED;
        }
        return storeDepartment.get().equals(deliveryDepartment.get())
                ? SCHEDULED_SAME_DEPARTMENT
                : SCHEDULED_DEPARTMENTAL;
    }

    private Optional<String> findDepartment(String ubigeoCod) {
        if (ubigeoCod == null || ubigeoCod.isBlank()) {
            return Optional.empty();
        }
        return ubigeoRepository.findDepartmentCodByUbigeoCod(ubigeoCod.trim());
    }

    private BusinessConfigEntity findActiveConfiguration(String scheduleType) {
        return businessConfigSearchService.findActivesByGroupCod(
                        BusinessConfigConstants.GroupCod.SHIPPING_SCHEDULE_CONFIG
                ).stream()
                .filter(item -> scheduleType.equals(item.ConfigCod))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe una configuración activa para " + scheduleType
                ));
    }

    private ShippingScheduleDto buildSchedule(
            BusinessConfigEntity config,
            int preparationTimeMinutes
    ) {
        validateConfiguration(config);
        ShippingScheduleDto result = new ShippingScheduleDto();
        result.ScheduleType = config.ConfigCod;
        result.ScheduleName = config.ConfigName;
        result.UseTimeSlot = normalizeIndicator(config.Sta1Config);

        ZonedDateTime now = ZonedDateTime.now(clock);
        int firstDayOffset = config.Num1Config;
        int lastDayOffset = config.Num2Config;
        for (int dayOffset = firstDayOffset; dayOffset <= lastDayOffset; dayOffset++) {
            LocalDate date = now.toLocalDate().plusDays(dayOffset);
            ShippingScheduleDateDto dateOption = buildDateOption(
                    config,
                    result.UseTimeSlot,
                    date,
                    now,
                    preparationTimeMinutes
            );
            if (dateOption != null) {
                result.DateList.add(dateOption);
            }
        }
        return result;
    }

    private ShippingScheduleDateDto buildDateOption(
            BusinessConfigEntity config,
            String useTimeSlot,
            LocalDate date,
            ZonedDateTime now,
            int preparationTimeMinutes
    ) {
        ShippingScheduleDateDto result = new ShippingScheduleDateDto();
        result.Date = date.toString();
        result.Label = buildDateLabel(date, now.toLocalDate());

        if (!"S".equals(useTimeSlot)) {
            result.ScheduledFrom = date.atStartOfDay(clock.getZone()).toInstant().toString();
            result.ScheduledTo = date.plusDays(1).atStartOfDay(clock.getZone())
                    .minusNanos(1)
                    .toInstant()
                    .toString();
            return result;
        }

        LocalTime startTime = parseTime(config.Str1Config, "inicio");
        LocalTime endTime = parseTime(config.Str2Config, "fin");
        ZonedDateTime minimumStart = now.plusMinutes(Math.max(0, preparationTimeMinutes));
        LocalTime currentStart = startTime;
        while (!currentStart.plusHours(config.Num3Config).isAfter(endTime)) {
            LocalTime currentEnd = currentStart.plusHours(config.Num3Config);
            ZonedDateTime scheduledFrom = date.atTime(currentStart).atZone(clock.getZone());
            ZonedDateTime scheduledTo = date.atTime(currentEnd).atZone(clock.getZone());
            if (!date.equals(now.toLocalDate()) || !scheduledFrom.isBefore(minimumStart)) {
                result.TimeSlotList.add(buildTimeSlot(currentStart, currentEnd, scheduledFrom, scheduledTo));
            }
            currentStart = currentEnd;
        }
        return result.TimeSlotList.isEmpty() ? null : result;
    }

    private ShippingScheduleTimeSlotDto buildTimeSlot(
            LocalTime startTime,
            LocalTime endTime,
            ZonedDateTime scheduledFrom,
            ZonedDateTime scheduledTo
    ) {
        ShippingScheduleTimeSlotDto result = new ShippingScheduleTimeSlotDto();
        result.StartTime = startTime.format(TIME_FORMAT);
        result.EndTime = endTime.format(TIME_FORMAT);
        result.Label = result.StartTime + " - " + result.EndTime;
        result.ScheduledFrom = scheduledFrom.toInstant().toString();
        result.ScheduledTo = scheduledTo.toInstant().toString();
        return result;
    }

    private String buildDateLabel(LocalDate date, LocalDate today) {
        String prefix = "";
        if (date.equals(today)) {
            prefix = "Hoy - ";
        } else if (date.equals(today.plusDays(1))) {
            prefix = "Mañana - ";
        }
        return prefix + date.format(DATE_LABEL_FORMAT);
    }

    private void validateConfiguration(BusinessConfigEntity config) {
        if (config.Num1Config == null || config.Num2Config == null
                || config.Num1Config < 0 || config.Num2Config < config.Num1Config) {
            throw new IllegalArgumentException(
                    "La configuración de días para " + config.ConfigCod + " no es válida"
            );
        }
        if ("S".equals(normalizeIndicator(config.Sta1Config))) {
            LocalTime startTime = parseTime(config.Str1Config, "inicio");
            LocalTime endTime = parseTime(config.Str2Config, "fin");
            if (!endTime.isAfter(startTime) || config.Num3Config == null || config.Num3Config <= 0) {
                throw new IllegalArgumentException(
                        "La configuración de horarios para " + config.ConfigCod + " no es válida"
                );
            }
            long configuredMinutes = Duration.between(startTime, endTime).toMinutes();
            if (configuredMinutes < config.Num3Config * 60L) {
                throw new IllegalArgumentException(
                        "El intervalo configurado no cabe en el horario de entrega"
                );
            }
        }
    }

    private String normalizeIndicator(String indicator) {
        return "S".equalsIgnoreCase(indicator) ? "S" : "N";
    }

    private LocalTime parseTime(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("La hora de " + fieldName + " no está configurada");
        }
        try {
            return LocalTime.parse(value.trim(), TIME_FORMAT);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("La hora de " + fieldName + " no es válida", ex);
        }
    }

    private Instant parseInstant(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("La fecha de " + fieldName + " de la entrega es obligatoria");
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("La fecha de " + fieldName + " no es válida", ex);
        }
    }

    private boolean matches(
            String candidateFrom,
            String candidateTo,
            Instant selectedFrom,
            Instant selectedTo
    ) {
        if (candidateFrom == null || candidateTo == null) {
            return false;
        }
        return Instant.parse(candidateFrom).equals(selectedFrom)
                && Instant.parse(candidateTo).equals(selectedTo);
    }

    private DeliveryCoverageRequestDto buildCoverageRequest(ShippingScheduleRequestDto request) {
        DeliveryCoverageRequestDto coverageRequest = new DeliveryCoverageRequestDto();
        coverageRequest.StoreCod = request.StoreCod;
        coverageRequest.DeliveryTypeCod = SaleConstants.DELIVERY_TYPE_SCHEDULED;
        coverageRequest.Latitude = request.Latitude;
        coverageRequest.Longitude = request.Longitude;
        return coverageRequest;
    }

    private void validateRequest(ShippingScheduleRequestDto request) {
        if (request == null || request.StoreCod == null || request.StoreCod.isBlank()) {
            throw new IllegalArgumentException("La tienda es obligatoria");
        }
        if (request.Latitude == null || request.Longitude == null) {
            throw new IllegalArgumentException("La dirección debe tener coordenadas válidas");
        }
    }
}
