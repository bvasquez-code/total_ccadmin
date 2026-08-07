package com.ccadmin.app.cash.service;

import com.ccadmin.app.cash.model.dto.CurrentCashSessionDto;
import com.ccadmin.app.cash.model.entity.CashRegisterEntity;
import com.ccadmin.app.cash.model.entity.CashSessionEntity;
import com.ccadmin.app.cash.repository.CashRegisterRepository;
import com.ccadmin.app.cash.repository.CashSessionItemRepository;
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
    private CashSessionItemRepository itemRepository;
    @Autowired
    private CashRegisterRepository cashRegisterRepository;

    @Autowired
    private SaleHeadRepository saleHeadRepository;

    @Transactional(readOnly = true)
    public CurrentCashSessionDto findCurrent() {
        CashRegisterEntity cashRegister = findCurrentCashRegister();
        CashSessionEntity cashSession = sessionRepository
                .findOpenByRegister(cashRegister.RegisterCod)
                .orElse(null);

        if (cashSession != null && !this.getUserCod().equals(cashSession.UserCod)) {
            throw new IllegalStateException("La caja se encuentra abierta por otro usuario");
        }

        return new CurrentCashSessionDto(cashRegister, cashSession);
    }

    @Transactional
    public CashSessionEntity open(String registerCod, String storeCod, String currencyCod,
                                  String commenter, java.math.BigDecimal openingFloat) {

        CashRegisterEntity cashRegister = findCurrentCashRegister();
        validateRequestedCashRegister(registerCod, storeCod, cashRegister);

        sessionRepository.findOpenByRegister(cashRegister.RegisterCod).ifPresent(s -> {
            throw new IllegalStateException("La caja ya tiene una sesión abierta");
        });

        CashSessionEntity s = new CashSessionEntity();
        s.RegisterCod = cashRegister.RegisterCod;
        s.StoreCod = cashRegister.StoreCod;
        s.UserCod = this.getUserCod();
        s.CurrencyCod = currencyCod;
        s.OpenDate = new Date();
        s.OpeningFloatAmount = openingFloat == null ? java.math.BigDecimal.ZERO : openingFloat;
        s.SessionStatus = 'O';
        s.IsOpen = 1;
        s.Commenter = commenter;

        s.validateOpen().session(this.getUserCod());
        return sessionRepository.save(s);
    }


    @Transactional
    public CashSessionEntity close(Long sessionId, String commenter) {
        CashSessionEntity s = sessionRepository.findByCashSessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada"));

        CashRegisterEntity cashRegister = findCurrentCashRegister();
        if (!this.getUserCod().equals(s.UserCod)
                || !cashRegister.StoreCod.equals(s.StoreCod)
                || !cashRegister.RegisterCod.equals(s.RegisterCod)) {
            throw new IllegalStateException("La sesión de caja no pertenece al usuario y tienda actuales");
        }

        if (s.IsOpen == 0 || s.SessionStatus != 'O')
            throw new IllegalStateException("La sesión ya está cerrada o cancelada");

        // ---- Calculados esperados (ventas + movimientos + fondo)
        BigDecimal expCash  = BigDecimal.ZERO;
        BigDecimal expOther = BigDecimal.ZERO;

        if (saleHeadRepository != null) {
            IExpectedTotalsDto totals = saleHeadRepository.getExpectedTotalsForSession(sessionId);
            if (totals != null) {
                expCash  = safe(totals.getCash());
                expOther = safe(totals.getOther());
            }
        }
        // Movimientos manuales (IN/OU) impactan el esperado
        BigDecimal netMovements = safe(itemRepository.sumNetMovements(sessionId));
        expCash = expCash.add(safe(s.OpeningFloatAmount)).add(netMovements); // caja suele impactar en efectivo
        BigDecimal expTotal = expCash.add(expOther);

        // ---- Contados (ingresados por el cajero)
        BigDecimal countedCashFromDenoms = safe(itemRepository.sumDenominations(sessionId));
        BigDecimal countedCashFromPayments = safe(itemRepository.sumCountedCashFromPayments(sessionId));
        BigDecimal countedCash = countedCashFromDenoms.add(countedCashFromPayments);

        BigDecimal countedOther = safe(itemRepository.sumCountedOther(sessionId));
        BigDecimal countedTotal = countedCash.add(countedOther);

        // ---- Diferencia
        BigDecimal difference = countedTotal.subtract(expTotal);

        // ---- Persistimos cierre
        s.ExpectedCashAmount  = expCash;
        s.ExpectedOtherAmount = expOther;
        s.ExpectedTotalAmount = expTotal;

        s.CountedCashAmount   = countedCash;
        s.CountedOtherAmount  = countedOther;
        s.CountedTotalAmount  = countedTotal;

        s.DifferenceAmount    = difference;

        s.SessionStatus = 'C';
        s.IsOpen = 0;
        s.CloseDate = new Date();
        s.Commenter = commenter;
        s.session(this.getUserCod());

        return sessionRepository.save(s);
    }

    private CashRegisterEntity findCurrentCashRegister() {
        String userCod = this.getUserCod();
        String storeCod = this.getStoreCod();

        return cashRegisterRepository.findActiveByUserAndStore(userCod, storeCod)
                .orElseThrow(() -> new IllegalStateException(
                        "El usuario actual no tiene una caja activa asignada en la tienda " + storeCod
                ));
    }

    private void validateRequestedCashRegister(String registerCod, String storeCod,
                                               CashRegisterEntity cashRegister) {
        if (registerCod != null && !registerCod.isBlank()
                && !cashRegister.RegisterCod.equals(registerCod)) {
            throw new IllegalArgumentException("La caja indicada no pertenece al usuario actual");
        }

        if (storeCod != null && !storeCod.isBlank()
                && !cashRegister.StoreCod.equals(storeCod)) {
            throw new IllegalArgumentException("La tienda indicada no corresponde a la sesión actual");
        }
    }

    private static BigDecimal safe(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}

