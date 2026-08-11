package com.ccadmin.app.delivery.service;

import com.ccadmin.app.delivery.model.dto.SalePaymentDeliveryRegisterDto;
import com.ccadmin.app.delivery.model.dto.SaleDeliveryAccessTokenPayloadDto;
import com.ccadmin.app.payment.model.entity.TrxPaymentEntity;
import com.ccadmin.app.payment.service.TrxPaymentCreateService;
import com.ccadmin.app.sale.model.dto.SalePaymentRegisterDto;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.model.entity.SalePaymentEntity;
import com.ccadmin.app.sale.repository.SaleHeadRepository;
import com.ccadmin.app.sale.repository.SalePaymentRepository;
import com.ccadmin.app.sale.service.SalePaymentCreateService;
import com.ccadmin.app.shared.model.dto.ClientSessionDto;
import com.ccadmin.app.shared.model.myconst.StatusConst;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class SalePaymentDeliveryCreateService {

    private final ClientDeliveryContextService clientDeliveryContextService;
    private final SaleHeadRepository saleHeadRepository;
    private final SalePaymentRepository salePaymentRepository;
    private final TrxPaymentCreateService trxPaymentCreateService;
    private final SalePaymentCreateService salePaymentCreateService;
    private final SaleDeliveryAccessTokenService saleDeliveryAccessTokenService;

    public SalePaymentDeliveryCreateService(
            ClientDeliveryContextService clientDeliveryContextService,
            SaleHeadRepository saleHeadRepository,
            SalePaymentRepository salePaymentRepository,
            TrxPaymentCreateService trxPaymentCreateService,
            SalePaymentCreateService salePaymentCreateService,
            SaleDeliveryAccessTokenService saleDeliveryAccessTokenService
    ) {
        this.clientDeliveryContextService = clientDeliveryContextService;
        this.saleHeadRepository = saleHeadRepository;
        this.salePaymentRepository = salePaymentRepository;
        this.trxPaymentCreateService = trxPaymentCreateService;
        this.salePaymentCreateService = salePaymentCreateService;
        this.saleDeliveryAccessTokenService = saleDeliveryAccessTokenService;
    }

    @Transactional
    public SalePaymentEntity save(SalePaymentDeliveryRegisterDto request) throws Exception {
        validateRequest(request);
        ClientSessionDto clientSession = clientDeliveryContextService.getCurrentClient();
        SaleDeliveryAccessTokenPayloadDto tokenPayload = saleDeliveryAccessTokenService.resolve(
                request.OrderToken,
                clientSession.ClientCod
        );
        SaleHeadEntity saleHead = saleHeadRepository.findWebSaleForUpdate(
                tokenPayload.SaleCod,
                clientSession.ClientCod
        ).orElseThrow(() -> new IllegalArgumentException(
                "La venta web no existe o no pertenece al cliente autenticado"
        ));

        if (!StatusConst.PENDING.equals(saleHead.SaleStatus)) {
            throw new IllegalArgumentException("La venta ya no se encuentra pendiente");
        }
        if (salePaymentRepository.countTotalPayment(saleHead.SaleCod) > 0) {
            throw new IllegalArgumentException("El pedido ya tiene un pago registrado");
        }

        prepareFullPayment(request.TrxPayment, saleHead);
        request.TrxPayment.TrxPaymentId = null;
        TrxPaymentEntity trxPayment = trxPaymentCreateService.saveWeb(request.TrxPayment);

        SalePaymentRegisterDto salePayment = new SalePaymentRegisterDto();
        salePayment.SaleCod = saleHead.SaleCod;
        salePayment.TrxPaymentId = trxPayment.TrxPaymentId;
        return salePaymentCreateService.saveWeb(salePayment, saleHead.StoreCod);
    }

    private void prepareFullPayment(
            TrxPaymentEntity trxPayment,
            SaleHeadEntity saleHead
    ) {
        if (saleHead.NumTotalPrice == null
                || saleHead.NumTotalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El pedido no tiene un monto valido para pagar");
        }
        trxPayment.CurrencyCod = saleHead.CurrencyCod;
        trxPayment.NumExchangevalue = saleHead.NumExchangevalue == null
                ? BigDecimal.ONE
                : saleHead.NumExchangevalue;
        trxPayment.AmountPaid = saleHead.NumTotalPrice;
        trxPayment.AmountReturned = BigDecimal.ZERO;
    }

    private void validateRequest(SalePaymentDeliveryRegisterDto request) {
        if (request == null || request.OrderToken == null || request.OrderToken.isBlank()) {
            throw new IllegalArgumentException("El acceso seguro al pedido es obligatorio");
        }
        if (request.TrxPayment == null) {
            throw new IllegalArgumentException("Los datos del pago son obligatorios");
        }
        if (!"I".equals(request.TrxPayment.TypeMovement)) {
            throw new IllegalArgumentException("La tienda virtual solo permite registrar ingresos");
        }
        if ("NC001".equals(request.TrxPayment.PaymentMethodCod)) {
            throw new IllegalArgumentException("La nota de credito no es un medio de pago manual");
        }
    }
}
