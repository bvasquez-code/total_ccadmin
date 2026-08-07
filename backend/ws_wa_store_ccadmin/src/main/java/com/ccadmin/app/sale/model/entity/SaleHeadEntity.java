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
    public Long CashSessionID;
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
    public String HasFiscalDocument = "N";
    public String IsPickingConfirmed = "N";

    @Transient
    public ClientEntity Client;

    public SaleHeadEntity()
    {

    }

    public SaleHeadEntity tax(BigDecimal NumTotalPriceNoTax,BigDecimal NumTotalTax){
        this.NumTotalPriceNoTax = NumTotalPriceNoTax;
        this.NumTotalTax = NumTotalTax;
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
