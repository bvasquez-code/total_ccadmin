package com.ccadmin.app.cash.service;

import com.ccadmin.app.cash.model.entity.CashSessionEntity;
import com.ccadmin.app.cash.repository.CashSessionItemRepository;
import com.ccadmin.app.cash.repository.CashSessionRepository;
import com.ccadmin.app.sale.model.idto.IExpectedTotalsDto;
import com.ccadmin.app.sale.repository.SaleHeadRepository;
import com.ccadmin.app.shared.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;

@Service
public class CashSessionAdminService extends SessionService {

    @Autowired
    private CashSessionRepository sessionRepository;
    @Autowired
    private CashSessionItemRepository itemRepository;

    @Autowired
    private SaleHeadRepository saleHeadRepository;

    public CashSessionEntity open(String registerCod, String storeCod, String currencyCod,
                                  String commenter, java.math.BigDecimal openingFloat) {

        sessionRepository.findOpenByRegister(registerCod).ifPresent(s -> {
            throw new IllegalStateException("La caja ya tiene una sesión abierta");
        });

        CashSessionEntity s = new CashSessionEntity();
        s.RegisterCod = registerCod;
        s.StoreCod = storeCod;
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


    public CashSessionEntity close(Long sessionId, String commenter) {
        CashSessionEntity s = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada"));

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

    private static BigDecimal safe(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}

