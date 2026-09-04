package com.ccadmin.app.inventory.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "stock_entry_head")
public class StockEntryHeadEntity extends AuditTableEntity implements Serializable {
    @Id
    public String StockEntryCod;
    public String StoreCod;
    public String ProcessType;
    public String MovementMode;
    public String ReasonCode;
    public String OriginStockEntryCod;
    public String ProcessStatus;
    public BigDecimal NumTotalPrice = BigDecimal.ZERO;
    public String Observation;
    public String ConfirmUser;
    public Date ConfirmDate;
    public String ResolutionUser;
    public Date ResolutionDate;
    @Transient
    public Boolean HasPendingResolution;
}
