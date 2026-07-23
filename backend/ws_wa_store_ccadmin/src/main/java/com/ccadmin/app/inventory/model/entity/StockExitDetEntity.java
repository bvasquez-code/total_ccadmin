package com.ccadmin.app.inventory.model.entity;

import com.ccadmin.app.inventory.model.entity.id.StockExitDetId;
import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "stock_exit_det")
@IdClass(StockExitDetId.class)
public class StockExitDetEntity extends AuditTableEntity implements Serializable {
    @Id
    public String StockExitCod;
    @Id
    public Integer ItemNumber;
    public String ProductCod;
    public String Variant;
    public String WarehouseCod;
    public String LotNumber;
    public Date ExpirationDate;
    public String ProductUnitName;
    public Integer ProductUnitFactor;
    public Integer NumUnit;
    public Integer NumUnitPending;
    public Integer NumUnitResolvedIn;
    public Integer NumUnitResolvedOut;
    public String UnavailableReasonCode;
    public String ResolvedInReasonCode;
    public String ResolvedOutReasonCode;
    public String ResolvedOutType;
    public Integer ResolutionVersion;
    public String OriginStockExitCod;
    public Integer OriginItemNumber;
    public String ResolutionType;
    public String ResolutionReasonCode;
    public String Observation;
    public Date NextReviewDate;
}
