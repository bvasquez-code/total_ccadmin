package com.ccadmin.app.sale.service;

import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.exception.SaleException;
import com.ccadmin.app.sale.model.dto.SaleDeliveryStatusChangeDto;
import com.ccadmin.app.sale.model.entity.SaleDeliveryEntity;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.model.entity.VirtualCartEntity;
import com.ccadmin.app.sale.repository.SaleDeliveryRepository;
import com.ccadmin.app.sale.repository.SaleHeadRepository;
import com.ccadmin.app.sale.repository.VirtualCartRepository;
import com.ccadmin.app.shared.service.SessionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.transaction.Transactional;

import java.util.Date;

@Service
public class SaleDeliveryCreateService extends SessionService {

    private final VirtualCartRepository virtualCartRepository;
    private final SaleDeliveryRepository saleDeliveryRepository;
    private final ObjectMapper objectMapper;
    @Autowired
    private SaleHeadRepository saleHeadRepository;

    public SaleDeliveryCreateService(
            VirtualCartRepository virtualCartRepository,
            SaleDeliveryRepository saleDeliveryRepository,
            ObjectMapper objectMapper
    ) {
        this.virtualCartRepository = virtualCartRepository;
        this.saleDeliveryRepository = saleDeliveryRepository;
        this.objectMapper = objectMapper;
    }

    public void createFromConvertedCart(String presaleCod, String saleCod, String userCod) {
        VirtualCartEntity cart = virtualCartRepository.findConvertedByPresaleCod(presaleCod)
                .orElse(null);
        if (cart == null) {
            return;
        }

        try {
            JsonNode deliveryData = objectMapper.readTree(cart.CartData).path("Delivery");
            if (deliveryData.isMissingNode() || deliveryData.isNull()) {
                throw new IllegalArgumentException("El carrito convertido no contiene datos de entrega");
            }

            SaleDeliveryEntity saleDelivery = objectMapper.treeToValue(
                    deliveryData,
                    SaleDeliveryEntity.class
            );
            if (saleDelivery.DeliveryTypeCod == null || saleDelivery.DeliveryTypeCod.isBlank()) {
                throw new IllegalArgumentException("El carrito convertido no indica modalidad de entrega");
            }
            saleDelivery.SaleCod = saleCod;
            saleDelivery.DeliveryStatus = SaleConstants.DELIVERY_STATUS_PENDING;
            saleDelivery.addSession(userCod);
            saleDeliveryRepository.save(saleDelivery);

            cart.SaleCod = saleCod;
            cart.addSession(userCod);
            virtualCartRepository.save(cart);
        } catch (Exception ex) {
            if (ex instanceof IllegalArgumentException illegalArgumentException) {
                throw illegalArgumentException;
            }
            throw new IllegalArgumentException("No se pudo crear la entrega de la venta", ex);
        }
    }

    @Transactional
    public SaleDeliveryEntity changeStatus(SaleDeliveryStatusChangeDto request) throws SaleException {
        if (request == null || request.SaleCod == null || request.SaleCod.isBlank()) {
            throw new SaleException("Debe indicar la venta web");
        }
        if (request.TargetStatus == null || request.TargetStatus.isBlank()) {
            throw new SaleException("Debe indicar el nuevo estado de entrega");
        }

        SaleHeadEntity saleHead = this.saleHeadRepository.findWebSaleBySaleCodForUpdate(request.SaleCod)
                .orElseThrow(() -> new SaleException("No existe la venta web " + request.SaleCod));
        SaleDeliveryEntity saleDelivery = this.saleDeliveryRepository
                .findActiveBySaleCodForUpdate(request.SaleCod)
                .orElseThrow(() -> new SaleException("La venta no tiene datos de entrega"));

        this.validateTransition(saleHead, saleDelivery, request);
        this.applyStatus(saleDelivery, request.TargetStatus, request.Commenter);
        saleDelivery.addSessionModify(getUserCod());
        return this.saleDeliveryRepository.save(saleDelivery);
    }

    private void validateTransition(
            SaleHeadEntity saleHead,
            SaleDeliveryEntity saleDelivery,
            SaleDeliveryStatusChangeDto request
    ) throws SaleException {
        String currentStatus = saleDelivery.DeliveryStatus;
        String targetStatus = request.TargetStatus;

        if (SaleConstants.DELIVERY_STATUS_CANCELLED.equals(targetStatus)) {
            throw new SaleException("La cancelacion debe realizarse mediante la anulacion de la venta");
        }
        if (SaleConstants.DELIVERY_STATUS_PENDING.equals(currentStatus)
                && SaleConstants.DELIVERY_STATUS_PREPARING.equals(targetStatus)) {
            if (!"S".equals(saleHead.IsPaid)) {
                throw new SaleException("El pedido debe estar pagado antes de iniciar su preparacion");
            }
            if (!SaleConstants.PENDING.equals(saleHead.SaleStatus)) {
                throw new SaleException("La venta no permite iniciar la preparacion");
            }
            return;
        }
        if (SaleConstants.DELIVERY_STATUS_PREPARING.equals(currentStatus)
                && SaleConstants.DELIVERY_STATUS_READY_FOR_PICKUP.equals(targetStatus)) {
            if (!SaleConstants.CONFIRMED.equals(saleHead.SaleStatus)
                    || !"S".equals(saleHead.HasFiscalDocument)) {
                throw new SaleException("Debe confirmar y facturar la venta antes de marcarla como lista");
            }
            return;
        }
        if (SaleConstants.DELIVERY_STATUS_READY_FOR_PICKUP.equals(currentStatus)
                && SaleConstants.DELIVERY_STATUS_DELIVERED.equals(targetStatus)
                && SaleConstants.DELIVERY_TYPE_STORE_PICKUP.equals(saleDelivery.DeliveryTypeCod)) {
            return;
        }
        if (SaleConstants.DELIVERY_STATUS_READY_FOR_PICKUP.equals(currentStatus)
                && SaleConstants.DELIVERY_STATUS_DISPATCHED.equals(targetStatus)
                && isDeliveryShipment(saleDelivery.DeliveryTypeCod)) {
            return;
        }
        if (SaleConstants.DELIVERY_STATUS_DISPATCHED.equals(currentStatus)
                && SaleConstants.DELIVERY_STATUS_DELIVERED.equals(targetStatus)
                && isDeliveryShipment(saleDelivery.DeliveryTypeCod)) {
            return;
        }
        if (SaleConstants.DELIVERY_STATUS_DISPATCHED.equals(currentStatus)
                && SaleConstants.DELIVERY_STATUS_FAILED.equals(targetStatus)
                && isDeliveryShipment(saleDelivery.DeliveryTypeCod)) {
            if (request.Commenter == null || request.Commenter.isBlank()) {
                throw new SaleException("Debe indicar el motivo de la entrega fallida");
            }
            return;
        }

        throw new SaleException(
                "No se permite cambiar la entrega de " + currentStatus + " a " + targetStatus
        );
    }

    private void applyStatus(
            SaleDeliveryEntity saleDelivery,
            String targetStatus,
            String commenter
    ) {
        Date operationDate = new Date();
        saleDelivery.DeliveryStatus = targetStatus;
        if (SaleConstants.DELIVERY_STATUS_READY_FOR_PICKUP.equals(targetStatus)) {
            saleDelivery.ReadyDate = operationDate;
        } else if (SaleConstants.DELIVERY_STATUS_DISPATCHED.equals(targetStatus)) {
            saleDelivery.DispatchDate = operationDate;
        } else if (SaleConstants.DELIVERY_STATUS_DELIVERED.equals(targetStatus)) {
            saleDelivery.DeliveredDate = operationDate;
        } else if (SaleConstants.DELIVERY_STATUS_FAILED.equals(targetStatus)) {
            saleDelivery.Commenter = commenter.trim();
        }
    }

    private boolean isDeliveryShipment(String deliveryTypeCod) {
        return SaleConstants.DELIVERY_TYPE_AUTOMATIC.equals(deliveryTypeCod)
                || SaleConstants.DELIVERY_TYPE_SCHEDULED.equals(deliveryTypeCod);
    }
}
