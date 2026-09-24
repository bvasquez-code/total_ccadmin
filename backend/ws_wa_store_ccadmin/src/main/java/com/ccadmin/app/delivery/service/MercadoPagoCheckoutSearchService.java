package com.ccadmin.app.delivery.service;

import com.ccadmin.app.delivery.model.dto.MercadoPagoCheckoutDto;
import com.ccadmin.app.delivery.model.dto.MercadoPagoResultDto;
import com.ccadmin.app.payment.repository.MercadoPagoAttemptRepository;
import com.ccadmin.app.payment.service.MercadoPagoConfigSearchService;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.repository.SaleHeadRepository;
import org.springframework.stereotype.Service;

@Service
public class MercadoPagoCheckoutSearchService {
    private final ClientDeliveryContextService clientDeliveryContextService;
    private final SaleDeliveryAccessTokenService saleDeliveryAccessTokenService;
    private final SaleHeadRepository saleHeadRepository;
    private final MercadoPagoConfigSearchService mercadoPagoConfigSearchService;
    private final MercadoPagoAttemptRepository mercadoPagoAttemptRepository;

    public MercadoPagoCheckoutSearchService(ClientDeliveryContextService clientDeliveryContextService,
            SaleDeliveryAccessTokenService saleDeliveryAccessTokenService, SaleHeadRepository saleHeadRepository,
            MercadoPagoConfigSearchService mercadoPagoConfigSearchService,
            MercadoPagoAttemptRepository mercadoPagoAttemptRepository) {
        this.clientDeliveryContextService = clientDeliveryContextService;
        this.saleDeliveryAccessTokenService = saleDeliveryAccessTokenService;
        this.saleHeadRepository = saleHeadRepository;
        this.mercadoPagoConfigSearchService = mercadoPagoConfigSearchService;
        this.mercadoPagoAttemptRepository = mercadoPagoAttemptRepository;
    }

    public SaleHeadEntity findOwnedSale(String orderToken) {
        var client = clientDeliveryContextService.getCurrentClient();
        var token = saleDeliveryAccessTokenService.resolve(orderToken, client.ClientCod);
        return saleHeadRepository.findWebSale(token.SaleCod, client.ClientCod)
                .orElseThrow(() -> new IllegalArgumentException("El pedido no pertenece al cliente autenticado"));
    }

    public MercadoPagoCheckoutDto findCheckout(String orderToken) {
        var sale = findOwnedSale(orderToken);
        var payment = mercadoPagoAttemptRepository.findLatest(sale.SaleCod)
                .map(MercadoPagoResultDto::from).orElse(null);
        var config = payment != null && "P".equals(payment.State)
                ? mercadoPagoConfigSearchService.findCredentials() : mercadoPagoConfigSearchService.findEnabled();
        if (!config.Str2Config.equals(sale.CurrencyCod)) {
            throw new IllegalArgumentException("La moneda del pedido no coincide con la cuenta de Mercado Pago");
        }
        var result = new MercadoPagoCheckoutDto();
        result.PublicKey = config.ConfigVal.trim();
        result.Locale = config.Str1Config == null ? "es-PE" : config.Str1Config;
        result.CurrencyCod = sale.CurrencyCod;
        result.Amount = sale.NumTotalPrice;
        result.MaxInstallments = config.Num1Config;
        result.TestMode = "S".equals(config.Sta2Config);
        result.Payment = payment;
        return result;
    }
}
