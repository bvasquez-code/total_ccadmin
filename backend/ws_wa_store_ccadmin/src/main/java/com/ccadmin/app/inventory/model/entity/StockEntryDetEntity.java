package com.ccadmin.app.inventory.model.entity;

import com.ccadmin.app.inventory.model.entity.id.StockEntryDetId;
import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "stock_entry_det")
@IdClass(StockEntryDetId.class)
public class StockEntryDetEntity extends AuditTableEntity implements Serializable {
    @Id
    public String StockEntryCod;
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
    public String OriginStockEntryCod;
    public Integer OriginItemNumber;
    public String ResolutionType;
    public String ResolutionReasonCode;
    public String Observation;
    public Date NextReviewDate;
}
