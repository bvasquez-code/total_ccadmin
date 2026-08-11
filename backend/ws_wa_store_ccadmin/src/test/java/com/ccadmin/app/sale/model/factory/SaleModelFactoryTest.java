package com.ccadmin.app.sale.model.factory;

import com.ccadmin.app.payment.model.entity.TrxPaymentEntity;
import com.ccadmin.app.sale.model.dto.SaleTaxCalculationResultDto;
import com.ccadmin.app.sale.model.entity.CreditNoteDetEntity;
import com.ccadmin.app.sale.model.entity.CreditNoteDetWarehouseEntity;
import com.ccadmin.app.sale.model.entity.PeriodEntity;
import com.ccadmin.app.sale.model.entity.PresaleDetEntity;
import com.ccadmin.app.sale.model.entity.PresaleHeadEntity;
import com.ccadmin.app.sale.model.entity.SaleDetEntity;
import com.ccadmin.app.sale.model.entity.SaleDetTaxEntity;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.model.entity.SalePaymentEntity;
import com.ccadmin.app.system.model.entity.CurrencyEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SaleModelFactoryTest {

    @Test
    void initializesUnpaidIndicatorWhenSavingAPresale() {
        PresaleHeadEntity presale = new PresaleHeadEntity();
        presale.CurrencyCod = "PEN";
        PeriodEntity period = new PeriodEntity();
        period.PeriodId = 202608;
        CurrencyEntity currency = new CurrencyEntity();
        currency.CurrencyCod = "PEN";

        PresaleHeadEntity result = PresaleHeadEntityFactory.fromSaveRequest(
                presale,
                period,
                currency,
                currency,
                "T001",
                "P"
        );

        assertEquals("N", result.IsPaid);
    }

    @Test
    void createsSaleHeadFromPresale() {
        PresaleHeadEntity presale = new PresaleHeadEntity();
        presale.PresaleCod = "PR001";
        presale.StoreCod = "T001";
        presale.ClientCod = "CL001";
        presale.NumPriceSubTotal = new BigDecimal("100.00");
        presale.NumDiscount = new BigDecimal("10.00");
        presale.NumTotalPrice = new BigDecimal("90.00");
        presale.NumTotalPriceNoTax = new BigDecimal("76.27");
        presale.NumTotalTax = new BigDecimal("13.73");
        presale.CurrencyCod = "PEN";
        presale.CurrencyCodSys = "PEN";
        presale.NumExchangevalue = BigDecimal.ONE;
        presale.IsPaid = "N";
        PeriodEntity period = new PeriodEntity();
        period.PeriodId = 202608;

        SaleHeadEntity result = SaleHeadEntityFactory.fromPresale(
                presale,
                period,
                "ST001",
                "P"
        );

        assertEquals("ST001", result.SaleCod);
        assertEquals(presale.PresaleCod, result.PresaleCod);
        assertEquals(presale.NumTotalPrice, result.NumTotalPrice);
        assertEquals(period.PeriodId, result.PeriodId);
        assertEquals("N", result.HasCreditNote);
        assertEquals("N", result.HasFiscalDocument);
        assertEquals("N", result.IsPickingConfirmed);
    }

    @Test
    void createsSaleDetailWithCalculatedTaxAmounts() {
        PresaleDetEntity presaleDetail = new PresaleDetEntity();
        presaleDetail.ItemNumber = 2;
        presaleDetail.ProductCod = "P001";
        presaleDetail.Variant = "0000";
        presaleDetail.NumUnit = 3;
        presaleDetail.NumUnitPrice = new BigDecimal("10.00");
        presaleDetail.NumDiscount = BigDecimal.ZERO;
        presaleDetail.NumUnitPriceSale = new BigDecimal("10.00");
        presaleDetail.NumTotalPrice = new BigDecimal("30.00");
        presaleDetail.ProductUnitName = "CJA";
        presaleDetail.ProductUnitFactor = 6;

        SaleDetEntity result = SaleDetEntityFactory.fromPresale(
                presaleDetail,
                "ST001",
                new BigDecimal("25.42"),
                new BigDecimal("4.58"),
                "S"
        );

        assertEquals("ST001", result.SaleCod);
        assertEquals(presaleDetail.ProductCod, result.ProductCod);
        assertEquals(new BigDecimal("25.42"), result.NumPriceSubTotal);
        assertEquals(new BigDecimal("4.58"), result.NumTotalTax);
        assertEquals("CJA", result.ProductUnitName);
        assertEquals(6, result.ProductUnitFactor);
    }

    @Test
    void createsPaymentAndReversal() throws Exception {
        SaleHeadEntity saleHead = new SaleHeadEntity();
        saleHead.SaleCod = "ST001";
        saleHead.CurrencyCodSys = "PEN";
        TrxPaymentEntity transaction = new TrxPaymentEntity();
        transaction.TrxPaymentId = 10L;
        transaction.CurrencyCod = "USD";
        transaction.NumExchangevalue = new BigDecimal("3.75");
        transaction.AmountPaid = new BigDecimal("20.00");

        SalePaymentEntity payment = SalePaymentEntityFactory.fromTransaction(
                saleHead,
                transaction,
                2,
                new BigDecimal("75.00"),
                new BigDecimal("-5.00")
        );
        TrxPaymentEntity reversalTransaction = new TrxPaymentEntity();
        reversalTransaction.TrxPaymentId = 11L;
        reversalTransaction.AmountPaid = new BigDecimal("-20.00");
        SalePaymentEntity reversal = SalePaymentEntityFactory.fromReversal(
                payment,
                reversalTransaction,
                "ADMIN",
                4
        );

        assertEquals(3, payment.PaymentNumber);
        assertEquals(BigDecimal.ZERO, payment.NumAmountReturned);
        assertEquals(4, reversal.PaymentNumber);
        assertEquals(payment.SaleCod, reversal.SaleCod);
        assertEquals(reversalTransaction.TrxPaymentId, reversal.TrxPaymentId);
        assertEquals("ADMIN", reversal.CreationUser);
        assertEquals("A", reversal.Status);
    }

    @Test
    void preservesProductUnitWhenCreatingReturnedStock() {
        CreditNoteDetEntity detail = new CreditNoteDetEntity();
        detail.CreditNoteCod = "NC001";
        detail.ItemNumber = 1;
        detail.ProductCod = "P001";
        detail.Variant = "0000";
        detail.NumUnitStockReturned = 12;
        detail.ProductUnitName = "CJA";
        detail.ProductUnitFactor = 6;

        CreditNoteDetWarehouseEntity result =
                CreditNoteDetWarehouseEntityFactory.fromReturnedDetail(
                        detail,
                        "ALM01"
                );

        assertEquals(12, result.NumUnit);
        assertEquals("CJA", result.ProductUnitName);
        assertEquals(6, result.ProductUnitFactor);
        assertEquals("ALM01", result.WarehouseCod);
    }

    @Test
    void assemblesTaxResultAndCalculatesTotals() {
        SaleDetEntity first = new SaleDetEntity();
        first.NumPriceSubTotal = new BigDecimal("10.10");
        first.NumTotalTax = new BigDecimal("1.82");
        first.NumTotalPrice = new BigDecimal("11.92");
        SaleDetEntity second = new SaleDetEntity();
        second.NumPriceSubTotal = new BigDecimal("20.20");
        second.NumTotalTax = new BigDecimal("3.64");
        second.NumTotalPrice = new BigDecimal("23.84");
        SaleDetTaxEntity tax = new SaleDetTaxEntity();

        SaleTaxCalculationResultDto result =
                SaleTaxCalculationResultDtoFactory.fromLines(
                        List.of(first, second),
                        List.of(tax)
                );

        assertEquals(new BigDecimal("30.30"), result.NumTotalPriceNoTax);
        assertEquals(new BigDecimal("5.46"), result.NumTotalTax);
        assertEquals(new BigDecimal("35.76"), result.NumTotalPrice);
        assertEquals(2, result.DetailList.size());
        assertEquals(1, result.TaxDetailList.size());
    }
}
