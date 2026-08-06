package com.ccadmin.app.pucharse.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table( name = "pucharse_head")
public class PucharseHeadEntity extends AuditTableEntity implements Serializable {

    @Id
    public String PucharseCod;
    public String StoreCod;
    public String PucharseReqCod;
    public String ExternalCod;
    public String DealerCod;
    public String Commenter;
    public String PurchaseStatus;
    public String CurrencyCod;
    public String CurrencyCodSys;
    public BigDecimal NumExchangevalue;
    public BigDecimal NumTotalPrice;

    public PucharseHeadEntity()
    {

    }
}
