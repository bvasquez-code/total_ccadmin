package com.ccadmin.app.sale.service;

import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.model.entity.SaleDeliveryEntity;
import com.ccadmin.app.sale.model.entity.VirtualCartEntity;
import com.ccadmin.app.sale.repository.SaleDeliveryRepository;
import com.ccadmin.app.sale.repository.VirtualCartRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class SaleDeliveryCreateService {

    private final VirtualCartRepository virtualCartRepository;
    private final SaleDeliveryRepository saleDeliveryRepository;
    private final ObjectMapper objectMapper;

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
}
