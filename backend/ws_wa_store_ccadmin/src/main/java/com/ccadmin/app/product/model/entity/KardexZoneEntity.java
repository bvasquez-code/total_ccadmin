package com.ccadmin.app.product.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "kardex_zone")
public class KardexZoneEntity extends AuditTableEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long KardexZoneID;
    public String OperationCod;
    public Integer ItemNumber;
    public String SourceTable;
    public String MovementEvent;
    public String ProductCod;
    public String Variant;
    public String StoreCod;
    public String WarehouseCod;
    public String ZoneStockMoved;
    public int NumStockMoved;
    public int NumZoneStockBefore;
    public int NumZoneStockAfter;
    public String LotNumber;
    public Date ExpirationDate;

    public KardexZoneEntity() {
    }
}
