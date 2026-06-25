package com.ccadmin.app.sale.model.entity;

import com.ccadmin.app.sale.exception.SaleBuildException;
import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "credit_note_head")
public class CreditNoteHeadEntity extends AuditTableEntity implements Serializable {

    @Id
    public String CreditNoteCod;
    public String SaleCod;
    public String StoreCod;
    public String ClientCod;
    public BigDecimal NumTotalPrice;
    public String Commenter;
    public Integer PeriodId;
    public String CreditNoteStatus;
    public String CurrencyCod;
    public String CurrencyCodSys;
    public BigDecimal NumExchangevalue;
    public String IsPaid;
    public String IsStockReturned;
    public String TypeCreditNote;

    public CreditNoteHeadEntity(){

    }

    public CreditNoteHeadEntity build(SaleHeadEntity saleHead,String CreditNoteStatus){
        this.SaleCod = saleHead.SaleCod;
        this.ClientCod = saleHead.ClientCod;
        this.CurrencyCod = saleHead.CurrencyCod;
        this.CurrencyCodSys = saleHead.CurrencyCodSys;
        this.PeriodId = saleHead.PeriodId;
        this.NumExchangevalue = saleHead.NumExchangevalue;
        this.CreditNoteStatus = CreditNoteStatus;
        return this;
    }

    public CreditNoteHeadEntity validate(){
        if(this.Commenter == null || this.Commenter.isEmpty()){
            throw new SaleBuildException("Es necesario incluir un motivo para la generación de una nota de crédito");
        }
        if(this.NumTotalPrice.compareTo(BigDecimal.ZERO) < 0){
            throw new SaleBuildException("El total debe ser mayor a cero");
        }
        return this;
    }

    @Override
    public CreditNoteHeadEntity session(String userCod) {
        this.addSession(userCod);
        return this;
    }
}
