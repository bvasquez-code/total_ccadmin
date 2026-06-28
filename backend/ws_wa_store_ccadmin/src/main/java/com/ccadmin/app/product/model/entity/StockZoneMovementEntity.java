package com.ccadmin.app.product.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "stock_zone_movement")
public class StockZoneMovementEntity extends AuditTableEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long StockZoneMovementId;
    public String OperationCod;
    public Integer ItemNumber;
    public String SourceTable;
    public String ProductCod;
    public String Variant;
    public String StoreCod;
    public String WarehouseCod;
    public String SourceZone;
    public String TargetZone;
    public Integer NumStockMoved;
    public Integer NumPhysicalStockBefore;
    public Integer NumPhysicalStockAfter;
    public Integer NumUnavailableStockBefore;
    public Integer NumUnavailableStockAfter;
    public Integer NumReservedStockBefore;
    public Integer NumReservedStockAfter;
    public Integer NumTotalStockBefore;
    public Integer NumTotalStockAfter;
    public String LotNumber;
    public Date ExpirationDate;

    public StockZoneMovementEntity(){

    }
}
