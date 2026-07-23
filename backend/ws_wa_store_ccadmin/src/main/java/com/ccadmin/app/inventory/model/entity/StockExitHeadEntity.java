package com.ccadmin.app.inventory.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "stock_exit_head")
public class StockExitHeadEntity extends AuditTableEntity implements Serializable {
    @Id
    public String StockExitCod;
    public String StoreCod;
    public String ProcessType;
    public String MovementMode;
    public String ReasonCode;
    public String OriginStockExitCod;
    public String ProcessStatus;
    public String Observation;
    public String ConfirmUser;
    public Date ConfirmDate;
    public String ResolutionUser;
    public Date ResolutionDate;
    @Transient
    public Boolean HasPendingResolution;
}
