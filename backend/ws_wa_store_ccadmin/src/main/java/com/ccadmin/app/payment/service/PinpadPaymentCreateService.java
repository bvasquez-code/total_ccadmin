package com.ccadmin.app.payment.service;

import com.ccadmin.app.payment.exception.PinpadPaymentException;
import com.ccadmin.app.payment.model.dto.PinpadPaymentStatusDto;
import com.ccadmin.app.payment.model.dto.PinpadBrowserInstructionsDto;
import com.ccadmin.app.payment.model.dto.PinpadSignedMessageDto;
import com.ccadmin.app.payment.model.entity.TrxPaymentEntity;
import com.ccadmin.app.payment.repository.TrxPaymentRepository;
import com.ccadmin.app.system.model.entity.PaymentMethodEntity;
import com.ccadmin.app.system.model.entity.CurrencyEntity;
import com.ccadmin.app.system.shared.CurrencyShared;
import com.ccadmin.app.system.shared.PaymentMethodShared;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@Slf4j
public class PinpadPaymentCreateService {
    private final PinpadConfigurationSearchService pinpadConfigurationSearchService;
    private final PinpadSignatureService pinpadSignatureService;
    private final ObjectMapper objectMapper;
    private final TrxPaymentRepository trxPaymentRepository;
    private final TrxPaymentPinpadCreateService trxPaymentPinpadCreateService;
    private final PaymentMethodShared paymentMethodShared;
    private final CurrencyShared currencyShared;
    private final PinpadTrustCreateService pinpadTrustCreateService;

    public PinpadPaymentCreateService(PinpadConfigurationSearchService pinpadConfigurationSearchService,
                                      PinpadSignatureService pinpadSignatureService, ObjectMapper objectMapper,
                                      TrxPaymentRepository trxPaymentRepository,
                                      TrxPaymentPinpadCreateService trxPaymentPinpadCreateService,
                                      PaymentMethodShared paymentMethodShared, CurrencyShared currencyShared,
                                      PinpadTrustCreateService pinpadTrustCreateService) {
        this.pinpadConfigurationSearchService = pinpadConfigurationSearchService;
        this.pinpadSignatureService = pinpadSignatureService;
        this.objectMapper = objectMapper;
        this.trxPaymentRepository = trxPaymentRepository;
        this.trxPaymentPinpadCreateService = trxPaymentPinpadCreateService;
        this.paymentMethodShared = paymentMethodShared;
        this.currencyShared = currencyShared;
        this.pinpadTrustCreateService = pinpadTrustCreateService;
    }

    public static boolean requiresPinpad(TrxPaymentEntity payment) {
        return "I".equals(payment.TypeMovement)
                && ("POS".equalsIgnoreCase(payment.PaymentPlatform)
                || "PINPAD".equalsIgnoreCase(payment.PaymentPlatform));
    }

    public PinpadBrowserInstructionsDto prepare(TrxPaymentEntity payment, String userCod, Long cashSessionId, String storeCod) {
        payment.PinpadPaymentId = reference(payment);
        String paymentMethod = validateRequest(payment, cashSessionId);
        var connection = pinpadConfigurationSearchService.connection(userCod, cashSessionId, storeCod);
        var existing = trxPaymentRepository.findByPinpadPaymentId(payment.PinpadPaymentId);
        if (existing != null) validateExisting(existing, payment, userCod, cashSessionId);
        else if (payment.TrxPaymentId != null && payment.TrxPaymentId > 0) {
            throw new IllegalArgumentException("No se puede modificar un pago existente mediante el pinpad");
        }
        Map<String, Object> command = command(payment, userCod, cashSessionId, connection, storeCod, paymentMethod, "REGISTER");
        PinpadSignedMessageDto login = pinpadSignatureService.sign(command, pinpadTrustCreateService.commandSigningKey());
        return new PinpadBrowserInstructionsDto(connection.loginUrl(), connection.registerUrl(), connection.statusUrl(), connection.ackUrl(),
                login, connection.waitSeconds(), connection.pollMillis());
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public TrxPaymentEntity pay(TrxPaymentEntity payment, String userCod, Long cashSessionId, String storeCod) {
        String paymentId = reference(payment);
        payment.PinpadPaymentId = paymentId;
        payment.CreationUser = userCod;
        String paymentMethod = validateRequest(payment, cashSessionId);
        var connection = pinpadConfigurationSearchService.connection(userCod, cashSessionId, storeCod);
        TrxPaymentEntity existing = trxPaymentRepository.findByPinpadPaymentId(paymentId);
        if (existing != null) {
            validateExisting(existing, payment, userCod, cashSessionId);
            return withAcknowledgement(existing, userCod, cashSessionId, connection, storeCod, paymentMethod);
        }
        if (payment.TrxPaymentId != null && payment.TrxPaymentId > 0) {
            throw new IllegalArgumentException("No se puede modificar un pago existente mediante el pinpad");
        }

        if (payment.PinpadResult == null) throw unresolved(paymentId, "UNKNOWN");
        JsonNode proof = pinpadSignatureService.verifyResult(payment.PinpadResult,
                pinpadTrustCreateService.agentPublicKey(connection.agentId()));
        Map<String, Object> expected = command(payment, userCod, cashSessionId, connection, storeCod, paymentMethod, "REGISTER");
        JsonNode issued = proof.path("command");
        boolean sameCashSession = cashSessionId == null ? issued.path("cashSessionId").isNull()
                : issued.path("cashSessionId").isIntegralNumber() && issued.path("cashSessionId").asLong() == cashSessionId;
        if (!connection.agentId().equals(proof.path("agentId").asText())
                || !connection.agentId().equals(issued.path("agentId").asText())
                || !sameCashSession
                || !storeCod.equals(issued.path("storeCod").asText())
                || !payment.CurrencyCod.equals(issued.path("currencyCod").asText())
                || !sameDecimal(payment.NumExchangevalue, issued.path("exchangeValue"))
                || !sameRequest(expected.get("request"), issued.path("request"))) {
            throw new IllegalArgumentException("El comprobante pinpad no corresponde a esta caja o solicitud");
        }
        PinpadPaymentStatusDto result;
        try {
            result = objectMapper.treeToValue(proof.path("result"), PinpadPaymentStatusDto.class);
        } catch (Exception exception) { throw unresolved(paymentId, "UNKNOWN"); }
        validateResult(result, paymentId, amountCents(payment),
                resolveCurrency(currencyShared.findById(payment.CurrencyCod)), userCod, payment.PaymentMethodCod);
        if (!"APPROVED".equals(result.status())) throw unresolved(paymentId, result.status());
        if (result.transactionId() == null || result.transactionId().isBlank()
                || result.transactionId().length() > 64) {
            throw unresolved(paymentId, "UNKNOWN");
        }
        TrxPaymentEntity saved;
        try {
            saved = trxPaymentPinpadCreateService.saveApproved(payment, result);
        } catch (DataIntegrityViolationException exception) {
            // A concurrent retry may have committed the same approval first (unique reference in SQL).
            saved = trxPaymentRepository.findByPinpadPaymentId(paymentId);
            if (saved == null) throw unresolved(paymentId, "APPROVED");
            validateExisting(saved, payment, userCod, cashSessionId);
        } catch (RuntimeException exception) {
            throw unresolved(paymentId, "APPROVED");
        }
        return withAcknowledgement(saved, userCod, cashSessionId, connection, storeCod, paymentMethod);
    }

    private Map<String, Object> command(TrxPaymentEntity payment, String userCod, Long cashSessionId,
            PinpadConfigurationSearchService.Connection connection, String storeCod, String paymentMethod, String operation) {
        Map<String, Object> command = new LinkedHashMap<>();
        command.put("operation", operation);
        command.put("agentId", connection.agentId());
        command.put("cashSessionId", cashSessionId);
        command.put("storeCod", storeCod);
        command.put("expiresAt", Instant.now().getEpochSecond() + 900);
        command.put("currencyCod", payment.CurrencyCod);
        command.put("exchangeValue", payment.NumExchangevalue == null ? null : payment.NumExchangevalue.toPlainString());
        command.put("request", Map.of("paymentId", payment.PinpadPaymentId, "amountCents", amountCents(payment),
                "currency", resolveCurrency(currencyShared.findById(payment.CurrencyCod)),
                "paymentMethod", paymentMethod, "internalPaymentCode", payment.PaymentMethodCod,
                "cashier", userCod, "externalReference", (cashSessionId == null ? "agent-" + connection.agentId() : "cash-session-" + cashSessionId) + "-" + payment.PinpadPaymentId));
        return command;
    }

    private long amountCents(TrxPaymentEntity payment) {
        try {
            if (payment.AmountPaid.compareTo(new BigDecimal("99999999999999.99")) > 0) throw new ArithmeticException();
            return payment.AmountPaid.movePointRight(2).longValueExact();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("El importe pinpad debe tener hasta dos decimales y estar dentro del rango permitido");
        }
    }

    private boolean sameDecimal(BigDecimal expected, JsonNode actual) {
        if (expected == null) return actual.isNull();
        try { return actual.isTextual() && expected.compareTo(new BigDecimal(actual.asText())) == 0; }
        catch (NumberFormatException exception) { return false; }
    }

    private boolean sameRequest(Object expected, JsonNode actual) {
        try { return objectMapper.readTree(objectMapper.writeValueAsBytes(expected)).equals(actual); }
        catch (Exception exception) { return false; }
    }

    private String reference(TrxPaymentEntity payment) {
        String paymentId = payment.PinpadPaymentId == null || payment.PinpadPaymentId.isBlank()
                ? payment.TransactionId : payment.PinpadPaymentId;
        if (paymentId == null || !paymentId.matches("[A-Za-z0-9._-]{8,96}")) {
            throw new IllegalArgumentException("El pago POS requiere un PinpadPaymentId estable (8..96 caracteres)");
        }
        return paymentId;
    }

    private String resolveCurrency(CurrencyEntity currency) {
        if (currency != null) {
            for (String candidate : new String[]{currency.CurrencyCod, currency.CurrencyAbbr}) {
                if (candidate != null && candidate.matches("[A-Z]{3}")) {
                    try {
                        return java.util.Currency.getInstance(candidate).getCurrencyCode();
                    } catch (IllegalArgumentException ignored) {
                        // Legacy catalogs may use internal identifiers; try their ISO abbreviation.
                    }
                }
            }
        }
        throw new IllegalArgumentException("La moneda debe tener un codigo o abreviatura ISO, por ejemplo PEN");
    }

    private String validateRequest(TrxPaymentEntity payment, Long cashSessionId) {
        PaymentMethodEntity method = paymentMethodShared.findById(payment.PaymentMethodCod);
        if (!requiresPinpad(payment)) {
            throw new IllegalArgumentException("El pinpad requiere un ingreso con plataforma POS o PINPAD");
        }
        if (method == null || !"A".equals(method.Status) || !"S".equals(method.IsInternalSaleEnabled)) {
            throw new IllegalArgumentException("El medio de pago debe estar habilitado para venta interna");
        }
        String paymentMethod = resolvePaymentMethod(method);
        if (payment.AmountPaid == null || payment.AmountPaid.signum() <= 0) {
            throw new IllegalArgumentException("El importe del pago pinpad debe ser positivo");
        }
        if (payment.AmountReturned != null && payment.AmountReturned.signum() != 0) {
            throw new IllegalArgumentException("El pago pinpad no admite vuelto");
        }
        payment.CashSessionID = cashSessionId;
        return paymentMethod;
    }

    private String resolvePaymentMethod(PaymentMethodEntity method) {
        if (method.PaymentMethodType != null) {
            switch (method.PaymentMethodType) {
                case "1002", "1003": return "CARD";
                case "1006": return "YAPE";
            }
        }
        throw new IllegalArgumentException("Este tipo de medio de pago no utiliza pinpad; registre el pago de forma manual");
    }

    private void validateExisting(TrxPaymentEntity existing, TrxPaymentEntity payment,
                                  String userCod, Long cashSessionId) {
        if (!Objects.equals(existing.CreationUser, userCod)
                || !Objects.equals(existing.CashSessionID, cashSessionId)
                || !Objects.equals(existing.PaymentMethodCod, payment.PaymentMethodCod)
                || !Objects.equals(existing.CurrencyCod, payment.CurrencyCod)
                || existing.AmountPaid.compareTo(payment.AmountPaid) != 0
                || !"A".equals(existing.Status) || !"OK".equals(existing.PaymentStatus)) {
            throw new IllegalArgumentException("La referencia pinpad ya pertenece a otro pago o sesion");
        }
    }

    private void validateResult(PinpadPaymentStatusDto result, String paymentId, long amountCents,
                                String currency, String userCod, String paymentMethodCod) {
        if (result == null || !paymentId.equals(result.paymentId())
                || !Long.valueOf(amountCents).equals(result.amountCents())
                || !currency.equals(result.currency()) || !userCod.equals(result.cashier())
                || !paymentMethodCod.equals(result.internalPaymentCode()) || result.status() == null) {
            throw unresolved(paymentId, "UNKNOWN");
        }
    }

    private TrxPaymentEntity withAcknowledgement(TrxPaymentEntity payment, String userCod, Long cashSessionId,
            PinpadConfigurationSearchService.Connection connection, String storeCod, String paymentMethod) {
        Map<String, Object> command = command(payment, userCod, cashSessionId, connection, storeCod, paymentMethod, "ACK");
        command.put("centralPaymentCod", payment.TrxPaymentId.toString());
        payment.PinpadResult = null;
        payment.PinpadAckCommand = pinpadSignatureService.sign(command, pinpadTrustCreateService.commandSigningKey());
        payment.PinpadAckUrl = connection.ackUrl();
        payment.PinpadLoginUrl = connection.loginUrl();
        return payment;
    }

    private PinpadPaymentException unresolved(String paymentId, String status) {
        String message = Set.of("REJECTED", "CANCELLED").contains(status)
                ? "El pinpad no aprobo el pago: " + status
                : "Pago pinpad pendiente de confirmar o guardar. Reintente con la misma referencia: " + paymentId;
        return new PinpadPaymentException(paymentId, status, message);
    }
}
