package com.ccadmin.app.payment.model.entity;

import com.ccadmin.app.payment.exception.TrxPaymentBuildException;
import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import com.ccadmin.app.shared.util.NumberUtil;
import jakarta.persistence.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "trx_payments")
public class TrxPaymentEntity extends AuditTableEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long TrxPaymentId;
    public String PaymentMethodCod;
    public String PaymentPlatform;
    public String CardNumber;
    public String CardHolderName;
    public Date CardExpirationDate;
    public String CardCVV;
    public String TransactionId;
    public String PaymentStatus;
    public String CurrencyCod;
    public String CurrencyCodSys;
    public BigDecimal NumExchangevalue;
    public BigDecimal AmountPaid;
    public BigDecimal AmountReturned;
    public String TypeMovement;
    public Long ReversalOfTrxPaymentId;

    public void validate(){

        if(NumberUtil.isLessOrEqual(this.AmountPaid,0) && TypeMovement.equals("I")){
            throw new TrxPaymentBuildException("Precio unitario no puede ser menor a cero.");
        }
    }

    public static TrxPaymentEntity buildReversal(TrxPaymentEntity originalPayment, String creationUser) {
        if (originalPayment == null) {
            throw new TrxPaymentBuildException("El pago original no puede ser nulo.");
        }

        TrxPaymentEntity reversal = new TrxPaymentEntity();
        reversal.TrxPaymentId = 0L;
        reversal.ReversalOfTrxPaymentId = originalPayment.TrxPaymentId;
        reversal.PaymentMethodCod = originalPayment.PaymentMethodCod;
        reversal.PaymentPlatform = originalPayment.PaymentPlatform;
        reversal.CardNumber = originalPayment.CardNumber;
        reversal.CardHolderName = originalPayment.CardHolderName;
        reversal.CardExpirationDate = originalPayment.CardExpirationDate;
        reversal.CardCVV = originalPayment.CardCVV;
        reversal.TransactionId = null;
        reversal.PaymentStatus = "OK";
        reversal.CurrencyCod = originalPayment.CurrencyCod;
        reversal.CurrencyCodSys = originalPayment.CurrencyCodSys;
        reversal.NumExchangevalue = originalPayment.NumExchangevalue;
        reversal.AmountPaid = originalPayment.AmountPaid.negate().add(originalPayment.AmountReturned);  // invierte el signo
        reversal.AmountReturned = BigDecimal.ZERO;
        reversal.TypeMovement = "E"; // Extorno
        reversal.CreationUser = creationUser;
        reversal.CreationDate = new Date();
        reversal.Status = "A";
        return reversal;
    }

    public static TrxPaymentEntity buildPartialReversal(TrxPaymentEntity originalPayment,BigDecimal amountToReverse,String userCod){
        if (originalPayment == null) {
            throw new TrxPaymentBuildException("El pago original no puede ser nulo.");
        }

        TrxPaymentEntity reversal = new TrxPaymentEntity();
        reversal.TrxPaymentId = 0L;
        reversal.ReversalOfTrxPaymentId = originalPayment.TrxPaymentId;
        reversal.PaymentMethodCod = originalPayment.PaymentMethodCod;
        reversal.PaymentPlatform = originalPayment.PaymentPlatform;
        reversal.CardNumber = originalPayment.CardNumber;
        reversal.CardHolderName = originalPayment.CardHolderName;
        reversal.CardExpirationDate = originalPayment.CardExpirationDate;
        reversal.CardCVV = originalPayment.CardCVV;
        reversal.TransactionId = null;
        reversal.PaymentStatus = "OK";
        reversal.CurrencyCod = originalPayment.CurrencyCod;
        reversal.CurrencyCodSys = originalPayment.CurrencyCodSys;
        reversal.NumExchangevalue = originalPayment.NumExchangevalue;
        reversal.AmountPaid = amountToReverse.negate();  // invierte el signo
        reversal.AmountReturned = BigDecimal.ZERO;
        reversal.TypeMovement = "E"; // Extorno
        reversal.CreationUser = userCod;
        reversal.CreationDate = new Date();
        reversal.Status = "A";
        return reversal;
    }
}
