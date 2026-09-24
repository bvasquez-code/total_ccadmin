package com.ccadmin.app.delivery.service;

import com.ccadmin.app.delivery.model.dto.MercadoPagoRequestDto;
import com.ccadmin.app.delivery.model.dto.MercadoPagoResultDto;
import com.ccadmin.app.payment.repository.MercadoPagoAttemptRepository;
import com.ccadmin.app.payment.service.MercadoPagoAttemptCreateService;
import com.ccadmin.app.payment.service.MercadoPagoClient;
import com.ccadmin.app.payment.service.MercadoPagoConfigSearchService;
import org.springframework.stereotype.Service;

@Service
public class MercadoPagoCheckoutCreateService {
    private final MercadoPagoCheckoutSearchService mercadoPagoCheckoutSearchService;
    private final MercadoPagoConfigSearchService mercadoPagoConfigSearchService;
    private final MercadoPagoAttemptCreateService mercadoPagoAttemptCreateService;
    private final MercadoPagoAttemptRepository mercadoPagoAttemptRepository;
    private final MercadoPagoClient mercadoPagoClient;

    public MercadoPagoCheckoutCreateService(MercadoPagoCheckoutSearchService mercadoPagoCheckoutSearchService,
            MercadoPagoConfigSearchService mercadoPagoConfigSearchService,
            MercadoPagoAttemptCreateService mercadoPagoAttemptCreateService,
            MercadoPagoAttemptRepository mercadoPagoAttemptRepository, MercadoPagoClient mercadoPagoClient) {
        this.mercadoPagoCheckoutSearchService = mercadoPagoCheckoutSearchService;
        this.mercadoPagoConfigSearchService = mercadoPagoConfigSearchService;
        this.mercadoPagoAttemptCreateService = mercadoPagoAttemptCreateService;
        this.mercadoPagoAttemptRepository = mercadoPagoAttemptRepository;
        this.mercadoPagoClient = mercadoPagoClient;
    }

    public MercadoPagoResultDto pay(MercadoPagoRequestDto request) throws Exception {
        var sale = mercadoPagoCheckoutSearchService.findOwnedSale(request.OrderToken);
        var config = mercadoPagoConfigSearchService.findEnabled();
        var preparation = mercadoPagoAttemptCreateService.prepare(sale.SaleCod, request, config);
        var attempt = preparation.attempt();
        if (!"P".equals(attempt.PaymentState)) return MercadoPagoResultDto.from(attempt);
        if (!preparation.created() && System.currentTimeMillis() - attempt.CreationDate.getTime() > 600000) {
            return refresh(request.OrderToken);
        }
        try {
            var payment = mercadoPagoClient.create(attempt, request.FormData, config);
            return MercadoPagoResultDto.from(mercadoPagoAttemptCreateService.apply(attempt.AttemptId, payment));
        } catch (MercadoPagoClient.PaymentRejectedException exception) {
            if (!preparation.created()) return refresh(request.OrderToken);
            var result = MercadoPagoResultDto.from(mercadoPagoAttemptCreateService.reject(attempt.AttemptId));
            if ("F".equals(result.State)) result.Message = exception.getMessage();
            return result;
        }
    }

    public MercadoPagoResultDto refresh(String orderToken) throws Exception {
        var sale = mercadoPagoCheckoutSearchService.findOwnedSale(orderToken);
        var attempt = mercadoPagoAttemptRepository.findLatest(sale.SaleCod).orElse(null);
        if (attempt == null) return null;
        if (!"P".equals(attempt.PaymentState)) return MercadoPagoResultDto.from(attempt);
        var payment = mercadoPagoClient.find(attempt, mercadoPagoConfigSearchService.findCredentials());
        return MercadoPagoResultDto.from(mercadoPagoAttemptCreateService.apply(attempt.AttemptId, payment));
    }
}
