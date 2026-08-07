package com.ccadmin.app.sale.model.entity;

import com.ccadmin.app.client.model.entity.ClientEntity;
import com.ccadmin.app.sale.exception.PresaleBuildException;
import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table( name = "presale_head" )
public class PresaleHeadEntity extends AuditTableEntity implements Serializable {

    @Id
    public String PresaleCod;
    public Long CashSessionID;
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

    @Transient
    public ClientEntity Client;
    @Transient
    public String RelatedSaleCod;
    @Transient
    public String RelatedSaleStatus;

    public PresaleHeadEntity(){

    }

    public boolean isEmptyPresaleCod(){
        return  ( this.PresaleCod == null || this.PresaleCod.trim().isEmpty());
    }

    public boolean existClient(){
        return (this.ClientCod != null && !this.ClientCod.isEmpty());
    }

    public PresaleHeadEntity validate() throws PresaleBuildException {

        if(this.PresaleCod==null || this.PresaleCod.isEmpty()){
            throw new PresaleBuildException("Código de preventa esta vació.");
        }
        return this;
    }

    @Override
    public PresaleHeadEntity session(String userCod) {
        this.addSession(userCod);
        return this;
    }

}
