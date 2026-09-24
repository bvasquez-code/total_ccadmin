package com.ccadmin.app.payment.service;

import com.ccadmin.app.payment.repository.MercadoPagoAttemptRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MercadoPagoReconciliationTaskService {
    private final MercadoPagoAttemptRepository mercadoPagoAttemptRepository;
    private final MercadoPagoConfigSearchService mercadoPagoConfigSearchService;
    private final MercadoPagoClient mercadoPagoClient;
    private final MercadoPagoAttemptCreateService mercadoPagoAttemptCreateService;

    public MercadoPagoReconciliationTaskService(MercadoPagoAttemptRepository mercadoPagoAttemptRepository,
            MercadoPagoConfigSearchService mercadoPagoConfigSearchService, MercadoPagoClient mercadoPagoClient,
            MercadoPagoAttemptCreateService mercadoPagoAttemptCreateService) {
        this.mercadoPagoAttemptRepository = mercadoPagoAttemptRepository;
        this.mercadoPagoConfigSearchService = mercadoPagoConfigSearchService;
        this.mercadoPagoClient = mercadoPagoClient;
        this.mercadoPagoAttemptCreateService = mercadoPagoAttemptCreateService;
    }

    // Reconcile even if the buyer closes the tab. Never creates a charge or depends on a client session.
    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void reconcile() {
        reconcilePending("SYSTEM");
    }

    public void reconcilePending(String userCod) {
        for (var attempt : mercadoPagoAttemptRepository.findPending()) {
            try {
                var payment = mercadoPagoClient.find(attempt, mercadoPagoConfigSearchService.findCredentials());
                mercadoPagoAttemptCreateService.apply(attempt.AttemptId, payment, userCod);
            } catch (Exception exception) {
                log.warn("No se pudo conciliar el intento de Mercado Pago {}", attempt.AttemptId);
            }
        }
    }
}
