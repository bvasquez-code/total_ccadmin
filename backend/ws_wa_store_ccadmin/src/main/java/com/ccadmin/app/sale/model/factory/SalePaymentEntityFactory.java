package com.ccadmin.app.sale.model.factory;

import com.ccadmin.app.payment.model.entity.TrxPaymentEntity;
import com.ccadmin.app.sale.exception.SalePaymentException;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.model.entity.SalePaymentEntity;

import java.math.BigDecimal;
import java.util.Date;

public final class SalePaymentEntityFactory {

    private SalePaymentEntityFactory() {
    }

    public static SalePaymentEntity fromTransaction(
            SaleHeadEntity saleHead,
            TrxPaymentEntity trxPayment,
            int currentPaymentCount,
            BigDecimal amountPaid,
            BigDecimal amountReturned
    ) {
        SalePaymentEntity payment = new SalePaymentEntity();
        payment.NumExchangevalue = trxPayment.NumExchangevalue;
        payment.PaymentNumber = currentPaymentCount + 1;
        payment.NumAmountPaidOrigin = trxPayment.AmountPaid;
        payment.CurrencyCod = trxPayment.CurrencyCod;
        payment.TrxPaymentId = trxPayment.TrxPaymentId;
        payment.SaleCod = saleHead.SaleCod;
        payment.CurrencyCodSys = saleHead.CurrencyCodSys;
        payment.NumAmountPaid = amountPaid;
        payment.NumAmountReturned = amountReturned.signum() < 0
                ? BigDecimal.ZERO
                : amountReturned;
        return payment;
    }

    public static SalePaymentEntity fromReversal(
            SalePaymentEntity originalPayment,
            TrxPaymentEntity trxPaymentReversal,
            String creationUser,
            int paymentNumber
    ) throws SalePaymentException {
        if (originalPayment == null) {
            throw new SalePaymentException("El pago original no puede ser nulo.");
        }
        if (trxPaymentReversal == null) {
            throw new SalePaymentException("El pago de reversión no puede ser nulo.");
        }

        SalePaymentEntity reversal = new SalePaymentEntity();
        reversal.PaymentNumber = paymentNumber;
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
