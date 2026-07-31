package com.ccadmin.app.sale.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "credit_note_application")
public class CreditNoteApplicationEntity extends AuditTableEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long ApplicationId;
    public String CreditNoteCod;
    public String SaleCod;
    public Long TrxPaymentId;
    public BigDecimal AmountApplied;

    @Override
    public CreditNoteApplicationEntity session(String userCod) {
        this.addSession(userCod);
        return this;
    }
}
