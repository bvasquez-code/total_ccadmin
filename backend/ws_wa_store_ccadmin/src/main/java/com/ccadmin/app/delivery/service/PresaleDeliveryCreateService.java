package com.ccadmin.app.delivery.service;

import com.ccadmin.app.delivery.model.dto.CheckoutDeliveryDto;
import com.ccadmin.app.delivery.model.dto.CheckoutConfirmationDto;
import com.ccadmin.app.delivery.model.dto.CheckoutRegisterDto;
import com.ccadmin.app.delivery.model.dto.DeliveryCoverageDto;
import com.ccadmin.app.delivery.model.dto.DeliveryCoverageRequestDto;
import com.ccadmin.app.product.model.entity.ProductConfigEntity;
import com.ccadmin.app.product.model.entity.ProductSearchEntity;
import com.ccadmin.app.product.service.ProductFindSearchService;
import com.ccadmin.app.product.shared.ProductOperationConfigShared;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.model.dto.PresaleDetailDto;
import com.ccadmin.app.sale.model.dto.PresaleRegisterDto;
import com.ccadmin.app.sale.model.dto.SaleDetailDto;
import com.ccadmin.app.sale.model.entity.ClientAddressEntity;
import com.ccadmin.app.sale.model.entity.PresaleDetEntity;
import com.ccadmin.app.sale.model.entity.VirtualCartEntity;
import com.ccadmin.app.sale.repository.ChannelDeliveryTypeRepository;
import com.ccadmin.app.sale.repository.VirtualCartRepository;
import com.ccadmin.app.sale.service.PresaleCreateService;
import com.ccadmin.app.shared.model.constants.AuditUserConstants;
import com.ccadmin.app.shared.model.dto.ClientSessionDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PresaleDeliveryCreateService {

    private static final long CONVERTED_CART_RETENTION_MILLISECONDS = 30L * 24L * 60L * 60L * 1000L;

    private final ClientDeliveryContextService clientDeliveryContextService;
    private final ClientAddressDeliverySearchService clientAddressDeliverySearchService;
    private final StoreDeliverySearchService storeDeliverySearchService;
    private final ProductFindSearchService productFindSearchService;
    private final ProductOperationConfigShared productOperationConfigShared;
    private final ChannelDeliveryTypeRepository channelDeliveryTypeRepository;
    private final PresaleCreateService presaleCreateService;
    private final VirtualCartRepository virtualCartRepository;
    private final ObjectMapper objectMapper;
    private final SaleDeliveryAccessTokenService saleDeliveryAccessTokenService;

    public PresaleDeliveryCreateService(
            ClientDeliveryContextService clientDeliveryContextService,
            ClientAddressDeliverySearchService clientAddressDeliverySearchService,
            StoreDeliverySearchService storeDeliverySearchService,
            ProductFindSearchService productFindSearchService,
            ProductOperationConfigShared productOperationConfigShared,
            ChannelDeliveryTypeRepository channelDeliveryTypeRepository,
            PresaleCreateService presaleCreateService,
            VirtualCartRepository virtualCartRepository,
            ObjectMapper objectMapper,
            SaleDeliveryAccessTokenService saleDeliveryAccessTokenService
    ) {
        this.clientDeliveryContextService = clientDeliveryContextService;
        this.clientAddressDeliverySearchService = clientAddressDeliverySearchService;
        this.storeDeliverySearchService = storeDeliverySearchService;
        this.productFindSearchService = productFindSearchService;
        this.productOperationConfigShared = productOperationConfigShared;
        this.channelDeliveryTypeRepository = channelDeliveryTypeRepository;
        this.presaleCreateService = presaleCreateService;
        this.virtualCartRepository = virtualCartRepository;
        this.objectMapper = objectMapper;
        this.saleDeliveryAccessTokenService = saleDeliveryAccessTokenService;
    }

    public String createCode(String storeCod) {
        storeDeliverySearchService.findActiveVirtualStore(storeCod);
        return presaleCreateService.createCodeWeb(storeCod);
    }

    @Transactional
    public CheckoutConfirmationDto confirm(PresaleRegisterDto request) throws Exception {
        ClientSessionDto clientSession = clientDeliveryContextService.getCurrentClient();
        if (request == null || request.Headboard == null
                || isBlank(request.Headboard.PresaleCod)
                || isBlank(request.Headboard.StoreCod)) {
            throw new IllegalArgumentException("La preventa y la tienda son obligatorias");
        }
        storeDeliverySearchService.findActiveVirtualStore(request.Headboard.StoreCod);
        SaleDetailDto saleDetail = presaleCreateService.confirmWeb(
                request,
                request.Headboard.StoreCod,
                clientSession.ClientCod
        );
        String orderToken = saleDeliveryAccessTokenService.issue(
                saleDetail.Headboard.SaleCod,
                clientSession.ClientCod
        );
        return new CheckoutConfirmationDto(orderToken, saleDetail);
    }

    @Transactional
    public PresaleDetailDto save(CheckoutRegisterDto request) {
        ClientSessionDto clientSession = clientDeliveryContextService.getCurrentClient();
        validateRequest(request);
        String storeCod = request.Headboard.StoreCod.trim();

        storeDeliverySearchService.findActiveVirtualStore(storeCod);
        validateDelivery(request.Delivery, storeCod, clientSession.ClientCod);
        validateDetails(request, storeCod);

        request.Headboard.ClientCod = clientSession.ClientCod;
        PresaleDetailDto result = presaleCreateService.saveWeb(request, storeCod);
        virtualCartRepository.save(buildConvertedCart(request, clientSession.ClientCod, result));
        return result;
    }

    private void validateDetails(CheckoutRegisterDto request, String storeCod) {
        Map<String, Integer> internalQuantityByProduct = new LinkedHashMap<>();
        for (PresaleDetEntity detail : request.DetailList) {
            validateDetailFields(detail);
            internalQuantityByProduct.merge(detail.ProductCod.trim(), detail.NumUnit, Math::addExact);

            ProductConfigEntity config = productOperationConfigShared.findByProduct(
                    detail.ProductCod,
                    storeCod
            );
            validateDetailConfiguration(detail, config, storeCod);
        }

        for (Map.Entry<String, Integer> item : internalQuantityByProduct.entrySet()) {
            String productCod = item.getKey();
            int internalQuantity = item.getValue();
            ProductSearchEntity availability = productFindSearchService.findAvailability(
                    productCod,
                    storeCod
            );
            ProductConfigEntity config = productOperationConfigShared.findByProduct(
                    productCod,
                    storeCod
            );
            if (!productOperationConfigShared.isDigital(config)
                    && internalQuantity > availability.NumPhysicalStock) {
                throw new IllegalArgumentException(
                        "No existe stock suficiente para el producto " + availability.ProductName
                );
            }
        }
    }

    private void validateDetailFields(PresaleDetEntity detail) {
        if (detail == null || isBlank(detail.ProductCod) || detail.NumUnit <= 0) {
            throw new IllegalArgumentException("Todos los productos deben tener código y cantidad válida");
        }
        if (isBlank(detail.Variant)) {
            throw new IllegalArgumentException("Todos los productos deben indicar una variante");
        }
        if (detail.NumUnitPrice == null || detail.NumUnitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Todos los productos deben indicar un precio válido");
        }
        if (detail.NumDiscount == null || detail.NumDiscount.compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("La tienda virtual todavía no admite descuentos en el carrito");
        }
    }

    private void validateDetailConfiguration(
            PresaleDetEntity detail,
            ProductConfigEntity config,
            String storeCod
    ) {
        if (!storeCod.equals(config.StoreCod) || config.NumPrice == null
                || config.NumPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "El producto " + detail.ProductCod + " no tiene una configuración válida en la tienda"
            );
        }
        if (detail.ProductUnitFactor != config.ProductUnitFactor
                || !config.ProductUnitName.equals(detail.ProductUnitName)) {
            throw new IllegalArgumentException(
                    "La unidad de venta del producto " + detail.ProductCod + " ya no es válida"
            );
        }
        if (detail.NumUnitPrice.compareTo(config.NumPrice) != 0) {
            throw new IllegalArgumentException(
                    "El precio del producto " + detail.ProductCod + " cambió. Actualice el carrito"
            );
        }
    }

    private void validateRequest(CheckoutRegisterDto request) {
        if (request == null || request.Headboard == null
                || isBlank(request.Headboard.StoreCod)) {
            throw new IllegalArgumentException("La tienda es obligatoria");
        }
        if (isBlank(request.Headboard.PresaleCod)) {
            throw new IllegalArgumentException("El código de preventa es obligatorio");
        }
        if (isBlank(request.Headboard.CurrencyCod)) {
            throw new IllegalArgumentException("La moneda de la preventa es obligatoria");
        }
        if (request.DetailList == null || request.DetailList.isEmpty()) {
            throw new IllegalArgumentException("El carrito no tiene productos");
        }
        if (request.PresaleChannel == null
                || !SaleConstants.COMMERCIAL_CHANNEL_WEB.equals(request.PresaleChannel.ChannelCod)) {
            throw new IllegalArgumentException("El canal de la preventa debe ser WEB");
        }
        if (request.Delivery == null || request.Delivery.DeliveryTypeCod == null
                || request.Delivery.DeliveryTypeCod.isBlank()) {
            throw new IllegalArgumentException("La modalidad de entrega es obligatoria");
        }
    }

    private void validateDelivery(
            CheckoutDeliveryDto delivery,
            String storeCod,
            String clientCod
    ) {
        channelDeliveryTypeRepository.findActiveByChannelAndDeliveryType(
                SaleConstants.COMMERCIAL_CHANNEL_WEB,
                delivery.DeliveryTypeCod
        ).orElseThrow(() -> new IllegalArgumentException(
                "La modalidad de entrega no está habilitada para la tienda virtual"
        ));

        if (!"S".equals(delivery.IsThirdParty) && !"N".equals(delivery.IsThirdParty)) {
            throw new IllegalArgumentException("El indicador de recojo por terceros debe ser S o N");
        }
        if (isBlank(delivery.Names) || isBlank(delivery.Phone)) {
            throw new IllegalArgumentException("El nombre y teléfono de quien recibe son obligatorios");
        }
        if ("S".equals(delivery.IsThirdParty) && isBlank(delivery.DocumentNumber)) {
            throw new IllegalArgumentException("El documento de la persona autorizada es obligatorio");
        }

        if (SaleConstants.DELIVERY_TYPE_STORE_PICKUP.equals(delivery.DeliveryTypeCod)) {
            clearDeliveryAddress(delivery);
        } else {
            ClientAddressEntity address = clientAddressDeliverySearchService.findActiveById(
                    clientCod,
                    delivery.ClientAddressID
            );
            applyAddressSnapshot(delivery, address);
        }

        DeliveryCoverageDto coverage = storeDeliverySearchService.validateCoverage(
                buildCoverageRequest(storeCod, delivery)
        );
        if (!"S".equals(coverage.IsAvailable)) {
            throw new IllegalArgumentException(coverage.Message);
        }
        delivery.EstimatedDistanceKm = coverage.DistanceKm;

        if (SaleConstants.DELIVERY_TYPE_AUTOMATIC.equals(delivery.DeliveryTypeCod)) {
            delivery.ScheduledFrom = null;
            delivery.ScheduledTo = null;
        }

        if (SaleConstants.DELIVERY_TYPE_SCHEDULED.equals(delivery.DeliveryTypeCod)) {
            Date scheduledFrom = parseDate(delivery.ScheduledFrom, "inicio");
            Date scheduledTo = parseDate(delivery.ScheduledTo, "fin");
            if (scheduledTo.before(scheduledFrom)) {
                throw new IllegalArgumentException(
                        "El final de la entrega programada no puede ser anterior al inicio"
                );
            }
            delivery.ScheduledFrom = scheduledFrom.toInstant().toString();
            delivery.ScheduledTo = scheduledTo.toInstant().toString();
        }
    }

    private DeliveryCoverageRequestDto buildCoverageRequest(
            String storeCod,
            CheckoutDeliveryDto delivery
    ) {
        DeliveryCoverageRequestDto request = new DeliveryCoverageRequestDto();
        request.StoreCod = storeCod;
        request.DeliveryTypeCod = delivery.DeliveryTypeCod;
        request.Latitude = delivery.Latitude;
        request.Longitude = delivery.Longitude;
        return request;
    }

    private void applyAddressSnapshot(
            CheckoutDeliveryDto delivery,
            ClientAddressEntity address
    ) {
        delivery.ClientAddressID = address.ClientAddressID;
        delivery.Address = address.Address;
        delivery.GeocodedAddress = address.GeocodedAddress;
        delivery.Reference = address.Reference;
        delivery.CountryCod = address.CountryCod;
        delivery.CountryName = address.CountryName;
        delivery.StateName = address.StateName;
        delivery.CityName = address.CityName;
        delivery.UbigeoCod = address.UbigeoCod;
        delivery.Latitude = address.Latitude;
        delivery.Longitude = address.Longitude;
    }

    private void clearDeliveryAddress(CheckoutDeliveryDto delivery) {
        delivery.ClientAddressID = null;
        delivery.Address = null;
        delivery.GeocodedAddress = null;
        delivery.Reference = null;
        delivery.CountryCod = null;
        delivery.CountryName = null;
        delivery.StateName = null;
        delivery.CityName = null;
        delivery.UbigeoCod = null;
        delivery.Latitude = null;
        delivery.Longitude = null;
        delivery.EstimatedDistanceKm = null;
        delivery.ScheduledFrom = null;
        delivery.ScheduledTo = null;
    }

    private Date parseDate(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("La fecha de " + fieldName + " de la entrega es obligatoria");
        }
        try {
            return Date.from(Instant.parse(value));
        } catch (DateTimeParseException ignored) {
            try {
                return Date.from(OffsetDateTime.parse(value).toInstant());
            } catch (DateTimeParseException ignoredOffset) {
                try {
                    return Date.from(LocalDateTime.parse(value)
                            .atZone(ZoneId.systemDefault())
                            .toInstant());
                } catch (DateTimeParseException ex) {
                    throw new IllegalArgumentException("La fecha de " + fieldName + " no es válida", ex);
                }
            }
        }
    }

    private VirtualCartEntity buildConvertedCart(
            CheckoutRegisterDto request,
            String clientCod,
            PresaleDetailDto presale
    ) {
        VirtualCartEntity cart = new VirtualCartEntity();
        cart.CartCod = UUID.randomUUID().toString();
        cart.ClientCod = clientCod;
        cart.StoreCod = request.Headboard.StoreCod;
        cart.PresaleCod = presale.Headboard.PresaleCod;
        cart.CartStatus = SaleConstants.CART_STATUS_CONVERTED;
        cart.ExpiresDate = new Date(System.currentTimeMillis() + CONVERTED_CART_RETENTION_MILLISECONDS);
        try {
            cart.CartData = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("No se pudo registrar la información de entrega", ex);
        }
        cart.addSession(AuditUserConstants.USER_WEB);
        return cart;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
