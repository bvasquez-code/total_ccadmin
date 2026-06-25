package com.ccadmin.app.sale.model.entity;

import com.ccadmin.app.payment.model.entity.TrxPaymentEntity;
import com.ccadmin.app.sale.exception.SalePaymentException;
import com.ccadmin.app.sale.model.entity.id.SalePaymentID;
import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table( name = "sale_payments")
@IdClass( SalePaymentID.class)
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

    public SalePaymentEntity(){

    }

    public SalePaymentEntity(SaleHeadEntity saleHead,TrxPaymentEntity trxPayment,int PaymentNumber,BigDecimal NumAmountPaid,BigDecimal NumAmountReturned){
        this.NumExchangevalue = trxPayment.NumExchangevalue;
        this.PaymentNumber = 1 + PaymentNumber;
        this.NumAmountPaidOrigin = trxPayment.AmountPaid;
        this.CurrencyCod = trxPayment.CurrencyCod;
        this.TrxPaymentId = trxPayment.TrxPaymentId;
        this.SaleCod = saleHead.SaleCod;
        this.CurrencyCodSys = saleHead.CurrencyCodSys;
        this.NumAmountPaid = NumAmountPaid;
        this.NumAmountReturned = (NumAmountReturned.doubleValue() < 0) ? new BigDecimal(0) : NumAmountReturned;
    }

    public SalePaymentEntity trxPayment(TrxPaymentEntity trxPayment,int PaymentNumber){
        this.NumExchangevalue = trxPayment.NumExchangevalue;
        this.PaymentNumber = 1 + PaymentNumber;
        this.NumAmountPaidOrigin = trxPayment.AmountPaid;
        this.CurrencyCod = trxPayment.CurrencyCod;
        this.TrxPaymentId = trxPayment.TrxPaymentId;
        this.NumAmountReturned = new BigDecimal(0);
        return this;
    }

    public SalePaymentEntity saleHead(SaleHeadEntity saleHead){
        this.SaleCod = saleHead.SaleCod;
        this.CurrencyCodSys = saleHead.CurrencyCodSys;
        return this;
    }

    public SalePaymentEntity numAmount(BigDecimal NumAmountPaid,BigDecimal NumAmountReturned){
        this.NumAmountPaid = NumAmountPaid;
        this.NumAmountReturned = (NumAmountReturned.doubleValue() < 0) ? new BigDecimal(0) : NumAmountReturned;
        return this;
    }

    public SalePaymentEntity build() throws SalePaymentException {
        return this;
    }

    public static SalePaymentEntity buildReversal(SalePaymentEntity originalPayment, TrxPaymentEntity trxPaymentReversal, String creationUser, int PaymentNumber) throws SalePaymentException {
        if (originalPayment == null) {
            throw new SalePaymentException("El pago original no puede ser nulo.");
        }
        if (trxPaymentReversal == null) {
            throw new SalePaymentException("El pago de reversión no puede ser nulo.");
        }

        SalePaymentEntity reversal = new SalePaymentEntity();
        reversal.PaymentNumber = PaymentNumber;
        reversal.SaleCod = originalPayment.SaleCod;
        reversal.TrxPaymentId = trxPaymentReversal.TrxPaymentId;
        reversal.CurrencyCod = originalPayment.CurrencyCod;
        reversal.CurrencyCodSys = originalPayment.CurrencyCodSys;
        reversal.NumExchangevalue = originalPayment.NumExchangevalue;
        reversal.NumAmountPaid = trxPaymentReversal.AmountPaid;
        reversal.NumAmountPaidOrigin = trxPaymentReversal.AmountPaid;
        reversal.NumAmountReturned = BigDecimal.ZERO;
        reversal.TrxPayment = trxPaymentReversal;
        reversal.CreationUser = creationUser;
        reversal.CreationDate = new Date();
        reversal.Status = "A";
        return reversal;
    }

}
