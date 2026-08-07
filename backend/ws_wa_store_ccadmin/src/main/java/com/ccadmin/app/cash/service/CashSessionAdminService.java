package com.ccadmin.app.cash.service;

import com.ccadmin.app.cash.model.dto.CurrentCashSessionDto;
import com.ccadmin.app.cash.model.entity.CashRegisterEntity;
import com.ccadmin.app.cash.model.entity.CashSessionEntity;
import com.ccadmin.app.cash.repository.CashRegisterRepository;
import com.ccadmin.app.cash.repository.CashSessionRepository;
import com.ccadmin.app.sale.model.idto.IExpectedTotalsDto;
import com.ccadmin.app.sale.repository.SaleHeadRepository;
import com.ccadmin.app.shared.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;

@Service
public class CashSessionAdminService extends SessionService {

    @Autowired
    private CashSessionRepository sessionRepository;
    @Autowired
    private CashRegisterRepository cashRegisterRepository;
    @Autowired
    private SaleHeadRepository saleHeadRepository;

    @Transactional
    public CurrentCashSessionDto findCurrent() {
        String storeCod = getStoreCod();
        CashSessionEntity cashSession = null;
        CashRegisterEntity cashRegister;

        Long cashSessionId = getCashSessionID();
        if (cashSessionId == null) {
            cashSessionId = sessionRepository.findOpenIdByUserAndStore(getUserCod(), storeCod)
                    .orElse(null);
            if (cashSessionId != null) {
                setCashSessionID(cashSessionId);
            }
        }
        if (cashSessionId != null) {
            cashSession = sessionRepository.findByCashSessionId(cashSessionId)
                    .orElseThrow(() -> new IllegalStateException("La sesión de caja del contexto ya no existe"));
            if (cashSession.IsOpen == 0 || cashSession.SessionStatus != 'O') {
                throw new IllegalStateException("La sesión de caja del contexto ya se encuentra cerrada");
            }
            if (!getUserCod().equals(cashSession.UserCod) || !storeCod.equals(cashSession.StoreCod)) {
                throw new IllegalStateException("La sesión de caja no pertenece al usuario y tienda actuales");
            }
            cashRegister = cashRegisterRepository.findActiveByRegisterCod(cashSession.RegisterCod)
                    .orElseThrow(() -> new IllegalStateException("La caja de la sesión ya no se encuentra activa"));
        } else {
            cashRegister = findCurrentUserCashRegister();
        }

        return new CurrentCashSessionDto(cashRegister, cashSession);
    }

    @Transactional(readOnly = true)
    public CashSessionEntity findById(Long cashSessionId) {
        if (cashSessionId == null || cashSessionId <= 0) {
            throw new IllegalArgumentException("El identificador de sesión de caja es obligatorio");
        }
        CashSessionEntity cashSession = sessionRepository.findByCashSessionId(cashSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Sesión de caja no encontrada"));
        if (!getStoreCod().equals(cashSession.StoreCod)) {
            throw new IllegalStateException("La sesión de caja no pertenece a la tienda actual");
        }
        return cashSession;
    }

    @Transactional
    public CashSessionEntity open(String currencyCod, String commenter, BigDecimal openingFloat) {
        if (getCashSessionID() != null) {
            throw new IllegalStateException("La sesión autenticada ya tiene una caja abierta");
        }

        CashRegisterEntity cashRegister = findCurrentUserCashRegister();

        sessionRepository.findOpenByRegister(cashRegister.RegisterCod).ifPresent(session -> {
            throw new IllegalStateException("La caja ya tiene una sesión abierta");
        });

        CashSessionEntity cashSession = new CashSessionEntity();
        cashSession.RegisterCod = cashRegister.RegisterCod;
        cashSession.StoreCod = cashRegister.StoreCod;
        cashSession.UserCod = getUserCod();
        cashSession.CurrencyCod = currencyCod;
        cashSession.OpenDate = new Date();
        cashSession.OpeningFloatAmount = openingFloat == null ? BigDecimal.ZERO : openingFloat;
        cashSession.SessionStatus = 'O';
        cashSession.IsOpen = 1;
        cashSession.Commenter = commenter;

        cashSession.validateOpen().session(getUserCod());
        CashSessionEntity saved = sessionRepository.save(cashSession);
        setCashSessionID(saved.CashSessionID);
        return saved;
    }

    @Transactional
    public CashSessionEntity close(String hasCashCount,
                                   BigDecimal countedCashAmount,
                                   BigDecimal countedOtherAmount,
                                   String commenter) {
        Long cashSessionId = getCashSessionID();
        if (cashSessionId == null) {
            throw new IllegalStateException("La sesión autenticada no tiene una caja abierta");
        }

        CashSessionEntity cashSession = sessionRepository.findByCashSessionId(cashSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada"));

        if (!getUserCod().equals(cashSession.UserCod) || !getStoreCod().equals(cashSession.StoreCod)) {
            throw new IllegalStateException("La sesión de caja no pertenece al usuario y tienda actuales");
        }
        if (cashSession.IsOpen == 0 || cashSession.SessionStatus != 'O') {
            throw new IllegalStateException("La sesión ya está cerrada o cancelada");
        }

        BigDecimal expectedCash = BigDecimal.ZERO;
        BigDecimal expectedOther = BigDecimal.ZERO;
        IExpectedTotalsDto totals = saleHeadRepository.getExpectedTotalsForSession(cashSessionId);
        if (totals != null) {
            expectedCash = safe(totals.getCash());
            expectedOther = safe(totals.getOther());
        }
        expectedCash = expectedCash.add(safe(cashSession.OpeningFloatAmount));
        BigDecimal expectedTotal = expectedCash.add(expectedOther);

        cashSession.ExpectedCashAmount = expectedCash;
        cashSession.ExpectedOtherAmount = expectedOther;
        cashSession.ExpectedTotalAmount = expectedTotal;

        boolean performCashCount = "S".equalsIgnoreCase(hasCashCount);
        cashSession.HasCashCount = performCashCount ? "S" : "N";
        if (performCashCount) {
            if (countedCashAmount == null || countedOtherAmount == null) {
                throw new IllegalArgumentException("Debe indicar los importes contados para realizar el arqueo");
            }
            BigDecimal countedCash = validateCountedAmount(countedCashAmount, "efectivo contado");
            BigDecimal countedOther = validateCountedAmount(countedOtherAmount, "otros medios contados");
            BigDecimal countedTotal = countedCash.add(countedOther);
            cashSession.CountedCashAmount = countedCash;
            cashSession.CountedOtherAmount = countedOther;
            cashSession.CountedTotalAmount = countedTotal;
            cashSession.DifferenceAmount = countedTotal.subtract(expectedTotal);
        } else {
            cashSession.CountedCashAmount = null;
            cashSession.CountedOtherAmount = null;
            cashSession.CountedTotalAmount = null;
            cashSession.DifferenceAmount = null;
        }

        cashSession.SessionStatus = 'C';
        cashSession.IsOpen = 0;
        cashSession.CloseDate = new Date();
        cashSession.Commenter = commenter;
        cashSession.session(getUserCod());

        CashSessionEntity saved = sessionRepository.save(cashSession);
        clearCashSessionID(cashSessionId);
        return saved;
    }

    private CashRegisterEntity findCurrentUserCashRegister() {
        return cashRegisterRepository.findActiveByUserAndStore(getUserCod(), getStoreCod())
                .orElseThrow(() -> new IllegalStateException(
                        "El usuario actual no tiene una caja activa asignada en la tienda " + getStoreCod()
                ));
    }

    private static BigDecimal validateCountedAmount(BigDecimal value, String fieldName) {
        BigDecimal amount = safe(value);
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("El " + fieldName + " no puede ser negativo");
        }
        return amount;
    }

    private static BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
