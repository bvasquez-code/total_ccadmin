package com.ccadmin.app.transfer.model.entity;

import com.ccadmin.app.product.model.entity.ProductEntity;
import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import com.ccadmin.app.transfer.exception.TransferException;
import com.ccadmin.app.transfer.model.entity.id.TransferDetId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.io.Serializable;

@Entity
@Table(name = "transfer_det")
@IdClass(TransferDetId.class)
public class TransferDetEntity extends AuditTableEntity implements Serializable {

    @Id
    public String TransferCod;
    public String TypeOperation;
    public String ProductCod;
    public String Variant;
    @Id
    public int ItemNumber;
    public String WarehouseCodOrigin;
    public String WarehouseCodDest;
    public int NumUnit;
    public int NumUnitDispatch;
    public int NumUnitReception;
    public String FlgRequested;
    public String LotNumber;
    public java.util.Date ExpirationDate;

    @Transient
    public ProductEntity Product;

    public TransferDetEntity() {
    }

    public TransferDetEntity validate() throws TransferException {
        if (this.TransferCod == null || this.TransferCod.trim().isEmpty()) {
            throw new TransferException("Código de transferencia está vacío");
        }
        if (this.TypeOperation == null || this.TypeOperation.trim().isEmpty()) {
            throw new TransferException("Tipo de operación es obligatorio");
        }
        if (this.ProductCod == null || this.ProductCod.trim().isEmpty()) {
            throw new TransferException("Código de producto es obligatorio");
        }
        if (this.Variant == null || this.Variant.trim().isEmpty()) {
            throw new TransferException("Variante de producto es obligatoria");
        }
        if (this.NumUnit <= 0) {
            throw new TransferException("La cantidad debe ser mayor a cero");
        }
        if (this.LotNumber != null && this.LotNumber.length() > 32) {
            throw new TransferException("El lote no puede superar 32 caracteres");
        }
        return this;
    }

    @Override
    public TransferDetEntity session(String userCod) {
        this.addSession(userCod);
        return this;
    }
}
