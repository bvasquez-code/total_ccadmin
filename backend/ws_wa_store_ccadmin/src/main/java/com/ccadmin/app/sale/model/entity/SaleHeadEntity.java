package com.ccadmin.app.sale.model.entity;

import com.ccadmin.app.client.model.entity.ClientEntity;
import com.ccadmin.app.sale.exception.SaleBuildException;
import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "sale_head")
public class SaleHeadEntity extends AuditTableEntity implements Serializable {

    @Id
    public String SaleCod;
    public String PresaleCod;
    public String StoreCod;
    public String ClientCod;
    public BigDecimal NumPriceSubTotal;
    public BigDecimal NumDiscount;
    public BigDecimal NumTotalPrice;
    public BigDecimal NumTotalPriceNoTax;
    public BigDecimal NumTotalTax;
    public String Commenter;
    public int PeriodId;
    public String SaleStatus;
    public String CurrencyCod;
    public String CurrencyCodSys;
    public BigDecimal NumExchangevalue;
    public String IsPaid;
    public String HasCreditNote;

    @Transient
    public ClientEntity Client;

    public SaleHeadEntity()
    {

    }

    public SaleHeadEntity(PresaleHeadEntity presaleHead)
    {
        PresaleCod = presaleHead.PresaleCod;
        StoreCod = presaleHead.StoreCod;
        ClientCod = presaleHead.ClientCod;
        NumPriceSubTotal = presaleHead.NumPriceSubTotal;
        NumDiscount = presaleHead.NumDiscount;
        NumTotalPrice = presaleHead.NumTotalPrice;
        NumTotalPriceNoTax = presaleHead.NumTotalPriceNoTax;
        NumTotalTax = presaleHead.NumTotalTax;
        Commenter = presaleHead.Commenter;
        PeriodId = presaleHead.PeriodId;
        SaleStatus = "P";
        CurrencyCod = presaleHead.CurrencyCod;
        CurrencyCodSys = presaleHead.CurrencyCodSys;
        NumExchangevalue = presaleHead.NumExchangevalue;
        IsPaid = presaleHead.IsPaid;
    }

    public SaleHeadEntity(String saleCod, String presaleCod, String storeCod, String clientCod, BigDecimal numPriceSubTotal, BigDecimal numDiscount, BigDecimal numTotalPrice, BigDecimal numTotalPriceNoTax, BigDecimal numTotalTax, String commenter, int periodId, String saleStatus, String currencyCod, String currencyCodSys, BigDecimal numExchangevalue, String isPaid) {
        SaleCod = saleCod;
        PresaleCod = presaleCod;
        StoreCod = storeCod;
        ClientCod = clientCod;
        NumPriceSubTotal = numPriceSubTotal;
        NumDiscount = numDiscount;
        NumTotalPrice = numTotalPrice;
        NumTotalPriceNoTax = numTotalPriceNoTax;
        NumTotalTax = numTotalTax;
        Commenter = commenter;
        PeriodId = periodId;
        SaleStatus = saleStatus;
        CurrencyCod = currencyCod;
        CurrencyCodSys = currencyCodSys;
        NumExchangevalue = numExchangevalue;
        IsPaid = isPaid;
    }

    public SaleHeadEntity tax(BigDecimal NumTotalPriceNoTax,BigDecimal NumTotalTax){
        this.NumTotalPriceNoTax = NumTotalPriceNoTax;
        this.NumTotalTax = NumTotalTax;
        return this;
    }



    public SaleHeadEntity build(PresaleHeadEntity presaleHead,PeriodEntity period,String SaleCod,String SaleStatus){
        this.SaleCod = SaleCod;
        this.PresaleCod = presaleHead.PresaleCod;
        this.StoreCod = presaleHead.StoreCod;
        this.ClientCod = presaleHead.ClientCod;
        this.NumPriceSubTotal = presaleHead.NumPriceSubTotal;
        this.NumDiscount = presaleHead.NumDiscount;
        this.NumTotalPrice = presaleHead.NumTotalPrice;
        this.NumTotalPriceNoTax = presaleHead.NumTotalPriceNoTax;
        this.NumTotalTax = presaleHead.NumTotalTax;
        this.Commenter = presaleHead.Commenter;
        this.SaleStatus = SaleStatus;
        this.CurrencyCod = presaleHead.CurrencyCod;
        this.CurrencyCodSys = presaleHead.CurrencyCodSys;
        this.NumExchangevalue = presaleHead.NumExchangevalue;
        this.IsPaid = presaleHead.IsPaid;
        this.PeriodId = period.PeriodId;
        this.HasCreditNote = "N";
        return this;
    }

    public SaleHeadEntity validate() throws SaleBuildException {
        if(this.SaleCod==null || this.SaleCod.isEmpty()){
            throw new SaleBuildException("Código de venta esta vacío");
        }
        if(this.NumPriceSubTotal.compareTo(BigDecimal.ZERO) < 0){
            throw new SaleBuildException("Sub total no puede ser negativo");
        }
        if(this.NumDiscount.compareTo(BigDecimal.ZERO) < 0){
            throw new SaleBuildException("Descuento no puede ser negativo");
        }
        if(this.NumTotalPrice.compareTo(BigDecimal.ZERO) < 0){
            throw new SaleBuildException("Precio total no puede ser negativo");
        }
        return this;
    }

    @Override
    public SaleHeadEntity session(String userCod) {
        this.addSession(userCod);
        return this;
    }

    public boolean existClient(){
        return (this.ClientCod != null && !this.CurrencyCod.isEmpty());
    }
}
