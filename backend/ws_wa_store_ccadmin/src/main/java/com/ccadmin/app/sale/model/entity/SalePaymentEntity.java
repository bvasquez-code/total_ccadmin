package com.ccadmin.app.sale.model.entity;

import com.ccadmin.app.payment.model.entity.TrxPaymentEntity;
import com.ccadmin.app.sale.model.entity.id.SalePaymentID;
import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "sale_payments")
@IdClass(SalePaymentID.class)
public class SalePaymentEntity extends AuditTableEntity implements Serializable {

    @Id
    public int PaymentNumber;
    @Id
    public String SaleCod;
    @Id
    public long TrxPaymentId;
    public String CurrencyCod;
    public String CurrencyCodSys;
    public BigDecimal NumExchangevalue;
    public BigDecimal NumAmountPaid;
    public BigDecimal NumAmountPaidOrigin;
    public BigDecimal NumAmountReturned;

    @Transient
    public TrxPaymentEntity TrxPayment;
}
