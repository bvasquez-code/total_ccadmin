package com.ccadmin.app.delivery.service;

import com.ccadmin.app.delivery.model.dto.CheckoutDeliveryDto;
import com.ccadmin.app.delivery.model.dto.CheckoutRegisterDto;
import com.ccadmin.app.delivery.model.dto.CheckoutConfirmationDto;
import com.ccadmin.app.delivery.model.dto.DeliveryCoverageDto;
import com.ccadmin.app.product.model.entity.ProductConfigEntity;
import com.ccadmin.app.product.model.entity.ProductSearchEntity;
import com.ccadmin.app.product.service.ProductFindSearchService;
import com.ccadmin.app.product.shared.ProductOperationConfigShared;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.model.dto.PresaleDetailDto;
import com.ccadmin.app.sale.model.dto.PresaleRegisterDto;
import com.ccadmin.app.sale.model.dto.SaleDetailDto;
import com.ccadmin.app.sale.model.entity.ChannelDeliveryTypeEntity;
import com.ccadmin.app.sale.model.entity.ClientAddressEntity;
import com.ccadmin.app.sale.model.entity.PresaleChannelEntity;
import com.ccadmin.app.sale.model.entity.PresaleDetEntity;
import com.ccadmin.app.sale.model.entity.PresaleHeadEntity;
import com.ccadmin.app.sale.repository.ChannelDeliveryTypeRepository;
import com.ccadmin.app.sale.repository.VirtualCartRepository;
import com.ccadmin.app.sale.service.PresaleCreateService;
import com.ccadmin.app.shared.model.dto.ClientSessionDto;
import com.ccadmin.app.store.model.entity.StoreEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresaleDeliveryCreateServiceTest {

    @Mock private ClientDeliveryContextService clientDeliveryContextService;
    @Mock private ClientAddressDeliverySearchService clientAddressDeliverySearchService;
    @Mock private StoreDeliverySearchService storeDeliverySearchService;
    @Mock private ProductFindSearchService productFindSearchService;
    @Mock private ProductOperationConfigShared productOperationConfigShared;
    @Mock private ChannelDeliveryTypeRepository channelDeliveryTypeRepository;
    @Mock private PresaleCreateService presaleCreateService;
    @Mock private VirtualCartRepository virtualCartRepository;
    @Mock private SaleDeliveryAccessTokenService saleDeliveryAccessTokenService;

    private PresaleDeliveryCreateService service;

    @BeforeEach
    void setUp() {
        service = new PresaleDeliveryCreateService(
                clientDeliveryContextService,
                clientAddressDeliverySearchService,
                storeDeliverySearchService,
                productFindSearchService,
                productOperationConfigShared,
                channelDeliveryTypeRepository,
                presaleCreateService,
                virtualCartRepository,
                new ObjectMapper(),
                saleDeliveryAccessTokenService
        );
    }

    @Test
    void validatesAndDelegatesTheCompleteFrontendPresaleToTheDomainCore() {
        CheckoutRegisterDto request = checkoutRequest();
        when(clientDeliveryContextService.getCurrentClient()).thenReturn(
                new ClientSessionDto(10L, "CL001", "client@example.com", "Cliente")
        );
        when(storeDeliverySearchService.findActiveVirtualStore("T001"))
                .thenReturn(new StoreEntity());
        DeliveryCoverageDto coverage = new DeliveryCoverageDto();
        coverage.IsAvailable = "S";
        coverage.DistanceKm = new BigDecimal("1.250");
        when(storeDeliverySearchService.validateCoverage(org.mockito.ArgumentMatchers.any()))
                .thenReturn(coverage);
        when(channelDeliveryTypeRepository.findActiveByChannelAndDeliveryType(
                SaleConstants.COMMERCIAL_CHANNEL_WEB,
                SaleConstants.DELIVERY_TYPE_AUTOMATIC
        )).thenReturn(Optional.of(new ChannelDeliveryTypeEntity()));
        ClientAddressEntity address = new ClientAddressEntity();
        address.ClientAddressID = 20L;
        address.Address = "Av. Principal 123";
        address.Reference = "Frente al parque";
        address.Latitude = new BigDecimal("-6.7812");
        address.Longitude = new BigDecimal("-79.8423");
        when(clientAddressDeliverySearchService.findActiveById("CL001", 20L))
                .thenReturn(address);

        ProductSearchEntity availability = new ProductSearchEntity();
        availability.ProductName = "Producto";
        availability.NumPhysicalStock = 20;
        when(productFindSearchService.findAvailability("P001", "T001"))
                .thenReturn(availability);
        ProductConfigEntity config = new ProductConfigEntity();
        config.ProductCod = "P001";
        config.StoreCod = "T001";
        config.NumPrice = new BigDecimal("12.50");
        config.ProductUnitName = "BOX";
        config.ProductUnitFactor = 2;
        config.IsDigital = "N";
        when(productOperationConfigShared.findByProduct("P001", "T001")).thenReturn(config);

        PresaleDetailDto saved = new PresaleDetailDto();
        saved.Headboard = new PresaleHeadEntity();
        saved.Headboard.PresaleCod = "PS001";
        when(presaleCreateService.saveWeb(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("T001")))
                .thenReturn(saved);

        service.save(request);

        ArgumentCaptor<PresaleRegisterDto> captor = ArgumentCaptor.forClass(PresaleRegisterDto.class);
        verify(presaleCreateService).saveWeb(captor.capture(), org.mockito.ArgumentMatchers.eq("T001"));
        PresaleRegisterDto delegated = captor.getValue();
        assertSame(request, delegated);
        assertEquals("CL001", delegated.Headboard.ClientCod);
        assertEquals("PEN", delegated.Headboard.CurrencyCod);
        assertEquals(SaleConstants.COMMERCIAL_CHANNEL_WEB, delegated.PresaleChannel.ChannelCod);
        assertEquals(6, delegated.DetailList.get(0).NumUnit);
        assertEquals(new BigDecimal("12.50"), delegated.DetailList.get(0).NumUnitPrice);
        assertEquals("Av. Principal 123", request.Delivery.Address);
        assertEquals(new BigDecimal("1.250"), request.Delivery.EstimatedDistanceKm);
        verify(virtualCartRepository).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void confirmsPresaleAndReturnsAnOpaqueOrderToken() throws Exception {
        PresaleRegisterDto request = new PresaleRegisterDto();
        request.Headboard = new PresaleHeadEntity();
        request.Headboard.PresaleCod = "PT001";
        request.Headboard.StoreCod = "T001";

        ClientSessionDto client = new ClientSessionDto(
                10L,
                "CL001",
                "client@example.com",
                "Cliente"
        );
        SaleDetailDto saleDetail = new SaleDetailDto();
        saleDetail.Headboard = new com.ccadmin.app.sale.model.entity.SaleHeadEntity();
        saleDetail.Headboard.SaleCod = "ST001";

        when(clientDeliveryContextService.getCurrentClient()).thenReturn(client);
        when(presaleCreateService.confirmWeb(request, "T001", "CL001"))
                .thenReturn(saleDetail);
        when(saleDeliveryAccessTokenService.issue("ST001", "CL001"))
                .thenReturn("v1.opaque-order-token");

        CheckoutConfirmationDto result = service.confirm(request);

        assertEquals("v1.opaque-order-token", result.OrderToken);
        assertSame(saleDetail, result.SaleDetail);
        verify(storeDeliverySearchService).findActiveVirtualStore("T001");
    }

    private CheckoutRegisterDto checkoutRequest() {
        PresaleDetEntity item = new PresaleDetEntity();
        item.PresaleCod = "PT001";
        item.ItemNumber = 1;
        item.ProductCod = "P001";
        item.Variant = "0000";
        item.NumUnit = 6;
        item.NumUnitPrice = new BigDecimal("12.50");
        item.NumDiscount = BigDecimal.ZERO;
        item.NumUnitPriceSale = new BigDecimal("12.50");
        item.NumTotalPrice = new BigDecimal("75.00");
        item.ProductUnitName = "BOX";
        item.ProductUnitFactor = 2;
        item.IsDigital = "N";

        CheckoutDeliveryDto delivery = new CheckoutDeliveryDto();
        delivery.DeliveryTypeCod = SaleConstants.DELIVERY_TYPE_AUTOMATIC;
        delivery.ClientAddressID = 20L;
        delivery.IsThirdParty = "N";
        delivery.Names = "Cliente";
        delivery.Phone = "999999999";

        CheckoutRegisterDto request = new CheckoutRegisterDto();
        request.Headboard = new PresaleHeadEntity();
        request.Headboard.PresaleCod = "PT001";
        request.Headboard.StoreCod = "T001";
        request.Headboard.CurrencyCod = "PEN";
        request.DetailList = List.of(item);
        request.PresaleChannel = new PresaleChannelEntity();
        request.PresaleChannel.PresaleCod = "PT001";
        request.PresaleChannel.ChannelCod = SaleConstants.COMMERCIAL_CHANNEL_WEB;
        request.Delivery = delivery;
        return request;
    }
}
