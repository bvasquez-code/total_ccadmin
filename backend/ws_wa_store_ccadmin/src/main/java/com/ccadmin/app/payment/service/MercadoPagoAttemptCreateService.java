package com.ccadmin.app.payment.service;

import com.ccadmin.app.delivery.model.dto.MercadoPagoRequestDto;
import com.ccadmin.app.delivery.service.SalePaymentDeliveryCreateService;
import com.ccadmin.app.payment.model.entity.MercadoPagoAttemptEntity;
import com.ccadmin.app.payment.model.entity.TrxPaymentEntity;
import com.ccadmin.app.payment.repository.MercadoPagoAttemptRepository;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.repository.SaleHeadRepository;
import com.ccadmin.app.sale.repository.SalePaymentRepository;
import com.ccadmin.app.shared.model.constants.AuditUserConstants;
import com.ccadmin.app.shared.model.entity.BusinessConfigEntity;
import com.ccadmin.app.system.shared.PaymentMethodShared;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

@Service
public class MercadoPagoAttemptCreateService {
    private final EntityManager entityManager;
    public record Preparation(MercadoPagoAttemptEntity attempt, boolean created) {}
    private final MercadoPagoAttemptRepository mercadoPagoAttemptRepository;
    private final SaleHeadRepository saleHeadRepository;
    private final SalePaymentRepository salePaymentRepository;
    private final SalePaymentDeliveryCreateService salePaymentDeliveryCreateService;
    private final PaymentMethodShared paymentMethodShared;
    private final ObjectMapper objectMapper;

    public MercadoPagoAttemptCreateService(MercadoPagoAttemptRepository mercadoPagoAttemptRepository,
            SaleHeadRepository saleHeadRepository, SalePaymentRepository salePaymentRepository,
            SalePaymentDeliveryCreateService salePaymentDeliveryCreateService,
            PaymentMethodShared paymentMethodShared, ObjectMapper objectMapper, EntityManager entityManager) {
        this.mercadoPagoAttemptRepository = mercadoPagoAttemptRepository;
        this.saleHeadRepository = saleHeadRepository;
        this.salePaymentRepository = salePaymentRepository;
        this.salePaymentDeliveryCreateService = salePaymentDeliveryCreateService;
        this.paymentMethodShared = paymentMethodShared;
        this.objectMapper = objectMapper;
        this.entityManager = entityManager;
    }

    // Commits BEFORE contacting the provider: a lost response cannot unlock a second charge.
    @Transactional(rollbackFor = Exception.class)
    public Preparation prepare(String saleCod, MercadoPagoRequestDto request,
                                           BusinessConfigEntity config) throws Exception {
        validateForm(request, config);
        SaleHeadEntity sale = lockSale(saleCod);
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(
                (request.PaymentMethodCod + objectMapper.writeValueAsString(request.FormData))
                        .getBytes(StandardCharsets.UTF_8)));
        var existing = mercadoPagoAttemptRepository.findById(request.AttemptId);
        if (existing.isPresent()) {
            var attempt = existing.get();
            entityManager.refresh(attempt, LockModeType.PESSIMISTIC_WRITE);
            if (!saleCod.equals(attempt.SaleCod) || !hash.equals(attempt.RequestHash)) {
                throw new IllegalArgumentException("El intento de pago no corresponde a estos datos");
            }
            if (!attempt.TestMode.equals(config.Sta2Config)) {
                throw new IllegalArgumentException("Resuelve el intento anterior antes de cambiar el modo de prueba");
            }
            return new Preparation(attempt, false);
        }
        if (!"P".equals(sale.SaleStatus) || "S".equals(sale.IsPaid)
                || salePaymentRepository.countTotalPayment(saleCod) > 0) {
            throw new IllegalArgumentException("El pedido ya no admite un nuevo pago");
        }
        if (mercadoPagoAttemptRepository.hasPendingPayment(saleCod)) {
            throw new IllegalArgumentException("Existe un pago en verificacion. Consulta su estado antes de reintentar");
        }
        paymentMethodShared.findActiveWebSaleById(request.PaymentMethodCod);
        if (sale.NumTotalPrice == null || sale.NumTotalPrice.signum() <= 0
                || !config.Str2Config.equals(sale.CurrencyCod)) {
            throw new IllegalArgumentException("El importe o la moneda del pedido no son validos para Mercado Pago");
        }
        var attempt = new MercadoPagoAttemptEntity();
        attempt.AttemptId = request.AttemptId;
        attempt.SaleCod = saleCod;
        attempt.PaymentMethodCod = request.PaymentMethodCod;
        attempt.RequestHash = hash;
        attempt.CredentialHash = MercadoPagoClient.credentialHash(config);
        attempt.Amount = sale.NumTotalPrice;
        attempt.CurrencyCod = sale.CurrencyCod;
        attempt.TestMode = config.Sta2Config;
        attempt.PaymentState = "P";
        attempt.addSession(AuditUserConstants.USER_WEB);
        return new Preparation(mercadoPagoAttemptRepository.saveAndFlush(attempt), true);
    }

    @Transactional(rollbackFor = Exception.class)
    public MercadoPagoAttemptEntity apply(String attemptId, JsonNode payment) throws Exception {
        return apply(attemptId, payment, AuditUserConstants.USER_WEB);
    }

    @Transactional(rollbackFor = Exception.class)
    public MercadoPagoAttemptEntity apply(String attemptId, JsonNode payment, String userCod) throws Exception {
        var reference = mercadoPagoAttemptRepository.findById(attemptId).orElseThrow();
        SaleHeadEntity sale = lockSale(reference.SaleCod);
        var attempt = mercadoPagoAttemptRepository.findById(attemptId).orElseThrow();
        entityManager.refresh(attempt, LockModeType.PESSIMISTIC_WRITE);
        if (!"P".equals(attempt.PaymentState)) return attempt;
        attempt.addSession(userCod);
        if (payment == null) return mercadoPagoAttemptRepository.save(attempt);
        validateResponse(attempt, payment);
        attempt.PaymentId = payment.path("id").asText();
        attempt.ProviderStatus = payment.path("status").asText();
        attempt.ProviderStatusDetail = payment.path("status_detail").asText(null);
        if ("approved".equals(attempt.ProviderStatus)) {
            if (sale.NumTotalPrice.compareTo(attempt.Amount) != 0 || !sale.CurrencyCod.equals(attempt.CurrencyCod)) {
                throw new IllegalStateException("El pedido cambio de importe. El pago requiere conciliacion");
            }
            var transaction = new TrxPaymentEntity();
            transaction.PaymentMethodCod = "debit_card".equals(payment.path("payment_type_id").asText()) ? "TD001" : "TC001";
            transaction.PaymentPlatform = "MERCADOPAGO";
            transaction.PaymentStatus = "OK";
            transaction.TypeMovement = "I";
            transaction.TransactionId = attempt.PaymentId;
            String lastFour = payment.path("card").path("last_four_digits").asText();
            transaction.CardNumber = lastFour.matches("[0-9]{4}") ? "************" + lastFour : null;
            // Unlock only within this transaction, together with the existing payment domain core.
            attempt.PaymentState = "C";
            mercadoPagoAttemptRepository.saveAndFlush(attempt);
            salePaymentDeliveryCreateService.saveApprovedGatewayPayment(sale, transaction);
        } else if (Set.of("rejected", "cancelled").contains(attempt.ProviderStatus)) {
            attempt.PaymentState = "F";
        }
        return mercadoPagoAttemptRepository.save(attempt);
    }

    @Transactional
    public MercadoPagoAttemptEntity reject(String attemptId) {
        var reference = mercadoPagoAttemptRepository.findById(attemptId).orElseThrow();
        lockSale(reference.SaleCod);
        var attempt = mercadoPagoAttemptRepository.findById(attemptId).orElseThrow();
        entityManager.refresh(attempt, LockModeType.PESSIMISTIC_WRITE);
        if ("P".equals(attempt.PaymentState) && attempt.PaymentId == null) {
            attempt.PaymentState = "F";
            attempt.ProviderStatus = "request_rejected";
            attempt.addSession(AuditUserConstants.USER_WEB);
        }
        return mercadoPagoAttemptRepository.save(attempt);
    }

    private SaleHeadEntity lockSale(String saleCod) {
        var sale = saleHeadRepository.findWebSaleBySaleCodForUpdate(saleCod)
                .orElseThrow(() -> new IllegalArgumentException("No existe el pedido web"));
        entityManager.refresh(sale, LockModeType.PESSIMISTIC_WRITE);
        return sale;
    }

    void validateResponse(MercadoPagoAttemptEntity attempt, JsonNode payment) {
        if (!payment.path("id").asText().matches("[0-9]+")
                || !attempt.AttemptId.equals(payment.path("external_reference").asText())
                || !payment.path("transaction_amount").isNumber()
                || attempt.Amount.compareTo(payment.path("transaction_amount").decimalValue()) != 0
                || !attempt.CurrencyCod.equals(payment.path("currency_id").asText())
                || !payment.path("live_mode").isBoolean()
                || payment.path("live_mode").asBoolean() == "S".equals(attempt.TestMode)
                || !Set.of("credit_card", "debit_card").contains(payment.path("payment_type_id").asText())
                || (attempt.PaymentId != null && !attempt.PaymentId.equals(payment.path("id").asText()))) {
            throw new IllegalStateException("La respuesta de Mercado Pago no coincide con el intento de pago");
        }
    }

    private void validateForm(MercadoPagoRequestDto request, BusinessConfigEntity config) {
        if (request == null || !("TC001".equals(request.PaymentMethodCod) || "TD001".equals(request.PaymentMethodCod))) {
            throw new IllegalArgumentException("Selecciona tarjeta de credito o debito");
        }
        if (request.AttemptId == null || !UUID.fromString(request.AttemptId).toString().equals(request.AttemptId)) {
            throw new IllegalArgumentException("El identificador del intento no es valido");
        }
        var form = request.FormData;
        if (form == null || form.token == null || !form.token.matches("[A-Za-z0-9_-]{1,256}")
                || form.payment_method_id == null || !form.payment_method_id.matches("[A-Za-z0-9_-]{1,64}")
                || form.installments == null || form.installments < 1 || form.installments > config.Num1Config
                || ("TD001".equals(request.PaymentMethodCod) && form.installments != 1)
                || form.payer == null || form.payer.email == null || form.payer.email.length() > 254
                || !form.payer.email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new IllegalArgumentException("Completa los datos de pago en el formulario de Mercado Pago");
        }
    }
}
