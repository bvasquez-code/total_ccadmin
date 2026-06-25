package com.ccadmin.app.cash.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "cash_session")
public class CashSessionEntity extends AuditTableEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long CashSessionID;

    public String RegisterCod;
    public String StoreCod;
    public String UserCod;

    public String CurrencyCod;

    @Temporal(TemporalType.TIMESTAMP)
    public Date OpenDate;
    @Temporal(TemporalType.TIMESTAMP)
    public Date CloseDate;

    public BigDecimal OpeningFloatAmount = BigDecimal.ZERO;

    public BigDecimal ExpectedCashAmount  = BigDecimal.ZERO;
    public BigDecimal ExpectedOtherAmount = BigDecimal.ZERO;
    public BigDecimal ExpectedTotalAmount = BigDecimal.ZERO;

    public BigDecimal CountedCashAmount   = BigDecimal.ZERO;
    public BigDecimal CountedOtherAmount  = BigDecimal.ZERO;
    public BigDecimal CountedTotalAmount  = BigDecimal.ZERO;

    public BigDecimal DifferenceAmount    = BigDecimal.ZERO;

    public Character SessionStatus = 'O'; // O/C/X
    public Integer IsOpen = 1;

    public String Commenter;

    public CashSessionEntity validateOpen() {
        if (RegisterCod == null || RegisterCod.isBlank())
            throw new IllegalArgumentException("RegisterCod requerido");
        if (StoreCod == null || StoreCod.isBlank())
            throw new IllegalArgumentException("StoreCod requerido");
        if (UserCod == null || UserCod.isBlank())
            throw new IllegalArgumentException("UserCod requerido");
        if (CurrencyCod == null || CurrencyCod.isBlank())
            throw new IllegalArgumentException("CurrencyCod requerido");
        if (OpenDate == null)
            throw new IllegalArgumentException("OpenDate requerido");
        return this;
    }

    public CashSessionEntity validateClose() {
        if (CashSessionID == null)
            throw new IllegalArgumentException("CashSessionID requerido");
        return this;
    }

    @Override
    public CashSessionEntity session(String userCod) {
        this.addSession(userCod);
        return this;
    }
}
