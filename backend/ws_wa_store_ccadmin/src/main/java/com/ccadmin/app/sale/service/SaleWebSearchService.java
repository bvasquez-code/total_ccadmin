package com.ccadmin.app.sale.service;

import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.model.dto.SaleWebOrderDto;
import com.ccadmin.app.sale.repository.SaleDeliveryRepository;
import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.shared.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class SaleWebSearchService extends SessionService {

    private static final int PAGE_LIMIT = 10;
    private static final Set<String> DELIVERY_TYPE_FILTERS = Set.of(
            SaleConstants.DELIVERY_TYPE_AUTOMATIC,
            SaleConstants.DELIVERY_TYPE_SCHEDULED,
            SaleConstants.DELIVERY_TYPE_STORE_PICKUP
    );
    private static final Set<String> DELIVERY_STATUS_FILTERS = Set.of(
            SaleConstants.DELIVERY_STATUS_PENDING,
            SaleConstants.DELIVERY_STATUS_SCHEDULED,
            SaleConstants.DELIVERY_STATUS_PREPARING,
            SaleConstants.DELIVERY_STATUS_READY_FOR_PICKUP,
            SaleConstants.DELIVERY_STATUS_DISPATCHED,
            SaleConstants.DELIVERY_STATUS_DELIVERED,
            SaleConstants.DELIVERY_STATUS_CANCELLED,
            SaleConstants.DELIVERY_STATUS_FAILED
    );

    @Autowired
    private SaleDeliveryRepository saleDeliveryRepository;

    public ResponsePageSearchT<SaleWebOrderDto> findAll(
            String query,
            int page,
            String deliveryTypeCod,
            String deliveryStatus
    ) {
        if (page < 1) {
            throw new IllegalArgumentException("La pagina debe ser mayor o igual a 1");
        }

        String normalizedQuery = query == null ? "" : query.trim();
        String normalizedDeliveryType = deliveryTypeCod == null ? "" : deliveryTypeCod.trim();
        String normalizedDeliveryStatus = deliveryStatus == null ? "" : deliveryStatus.trim();
        if (!normalizedDeliveryType.isEmpty() && !DELIVERY_TYPE_FILTERS.contains(normalizedDeliveryType)) {
            throw new IllegalArgumentException("La modalidad de entrega no es valida");
        }
        if (!normalizedDeliveryStatus.isEmpty() && !DELIVERY_STATUS_FILTERS.contains(normalizedDeliveryStatus)) {
            throw new IllegalArgumentException("El estado de entrega no es valido");
        }

        int init = (page - 1) * PAGE_LIMIT;
        String storeCod = getStoreCod();
        List<SaleWebOrderDto> orderList = this.saleDeliveryRepository.findWebOrders(
                normalizedQuery,
                storeCod,
                normalizedDeliveryType,
                normalizedDeliveryStatus,
                init,
                PAGE_LIMIT
        ).stream().map(SaleWebOrderDto::from).toList();
        int totalResult = this.saleDeliveryRepository.countWebOrders(
                normalizedQuery,
                storeCod,
                normalizedDeliveryType,
                normalizedDeliveryStatus
        );
        return new ResponsePageSearchT<>(orderList, page, PAGE_LIMIT, totalResult);
    }
}
