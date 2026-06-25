package com.ccadmin.app.sale.model.entity;

import com.ccadmin.app.sale.exception.PresaleBuildException;
import com.ccadmin.app.sale.model.entity.id.PresaleDetWarehouseID;
import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import com.ccadmin.app.store.model.entity.WarehouseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table( name = "presale_det_warehouse" )
@IdClass(PresaleDetWarehouseID.class)
public class PresaleDetWarehouseEntity extends AuditTableEntity implements Serializable {

    @Id
    public String PresaleCod;
    @Id
    public int ItemNumber;
    public String ProductCod;
    public String Variant;
    public String WarehouseCod;
    public int NumUnit;
    public String LotNumber;
    public Date ExpirationDate;

    public PresaleDetWarehouseEntity(){

    }

    public PresaleDetWarehouseEntity build(PresaleDetEntity presaleDet,WarehouseEntity warehouseDefault){
        this.PresaleCod = presaleDet.PresaleCod;
        this.ItemNumber = presaleDet.ItemNumber;
        this.ProductCod = presaleDet.ProductCod;
        this.Variant = presaleDet.Variant;
        this.NumUnit = presaleDet.NumUnit;
        this.WarehouseCod = warehouseDefault.WarehouseCod;
        this.LotNumber = presaleDet.LotNumber;
        this.ExpirationDate = presaleDet.ExpirationDate;
        return this;
    }

    public PresaleDetWarehouseEntity validate() throws PresaleBuildException {
        if(this.PresaleCod==null || this.PresaleCod.isEmpty()){
            throw new PresaleBuildException("Código de preventa esta vació.");
        }
        if(this.NumUnit == 0){
            throw new PresaleBuildException("Número de productos no puede ser cero.");
        }
        return this;
    }

    @Override
    public PresaleDetWarehouseEntity session(String userCod) {
        this.addSession(userCod);
        return this;
    }
}
