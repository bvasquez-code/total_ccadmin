package com.ccadmin.app.delivery.service;

import com.ccadmin.app.sale.repository.SaleHeadRepository;
import com.ccadmin.app.delivery.model.dto.SaleDeliveryAccessTokenPayloadDto;
import com.ccadmin.app.delivery.model.dto.SaleDeliveryOrderDto;
import com.ccadmin.app.sale.model.idto.ISaleDeliveryOrderDto;
import com.ccadmin.app.sale.service.SaleSearchService;
import com.ccadmin.app.shared.model.dto.ClientSessionDto;
import com.ccadmin.app.shared.model.dto.ResponseAdditionalDto;
import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.system.model.entity.PaymentMethodEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SaleDeliverySearchService {

    private static final int ORDER_PAGE_SIZE = 10;

    private final ClientDeliveryContextService clientDeliveryContextService;
    private final SaleHeadRepository saleHeadRepository;
    private final SaleSearchService saleSearchService;
    private final SaleDeliveryAccessTokenService saleDeliveryAccessTokenService;

    public SaleDeliverySearchService(
            ClientDeliveryContextService clientDeliveryContextService,
            SaleHeadRepository saleHeadRepository,
            SaleSearchService saleSearchService,
            SaleDeliveryAccessTokenService saleDeliveryAccessTokenService
    ) {
        this.clientDeliveryContextService = clientDeliveryContextService;
        this.saleHeadRepository = saleHeadRepository;
        this.saleSearchService = saleSearchService;
        this.saleDeliveryAccessTokenService = saleDeliveryAccessTokenService;
    }

    public ResponseWsDto findDataForm(String orderToken) {
        ClientSessionDto clientSession = clientDeliveryContextService.getCurrentClient();
        SaleDeliveryAccessTokenPayloadDto tokenPayload = saleDeliveryAccessTokenService.resolve(
                orderToken,
                clientSession.ClientCod
        );
        saleHeadRepository.findWebSale(tokenPayload.SaleCod, clientSession.ClientCod)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La venta web no existe o no pertenece al cliente autenticado"
                ));

        ResponseWsDto response = saleSearchService.findDataForm(tokenPayload.SaleCod);
        ResponseAdditionalDto paymentMethods = response.DataAdditional.stream()
                .filter(item -> "PaymentMethodList".equals(item.Name))
                .findFirst()
                .orElse(null);
        if (paymentMethods != null && paymentMethods.Data instanceof List<?> list) {
            paymentMethods.Data = list.stream()
                    .filter(PaymentMethodEntity.class::isInstance)
                    .map(PaymentMethodEntity.class::cast)
                    .filter(item -> !"NC001".equals(item.PaymentMethodCod))
                    .toList();
        }
        return response;
    }

    public ResponsePageSearchT<SaleDeliveryOrderDto> findMyOrders(int page) {
        int currentPage = Math.max(1, page);
        int init = (currentPage - 1) * ORDER_PAGE_SIZE;
        ClientSessionDto clientSession = clientDeliveryContextService.getCurrentClient();
        int totalResult = saleHeadRepository.countWebSalesByClientCod(clientSession.ClientCod);
        List<SaleDeliveryOrderDto> orderList = saleHeadRepository.findWebSalesByClientCod(
                        clientSession.ClientCod,
                        init,
                        ORDER_PAGE_SIZE
                ).stream()
                .map(source -> buildOrder(source, clientSession.ClientCod))
                .toList();
        return new ResponsePageSearchT<>(
                orderList,
                currentPage,
                ORDER_PAGE_SIZE,
                totalResult
        );
    }

    private SaleDeliveryOrderDto buildOrder(
            ISaleDeliveryOrderDto source,
            String clientCod
    ) {
        SaleDeliveryOrderDto order = SaleDeliveryOrderDto.from(source);
        if (order.CanResumePayment) {
            order.OrderToken = saleDeliveryAccessTokenService.issue(
                    order.SaleCod,
                    clientCod
            );
        }
        return order;
    }
}
