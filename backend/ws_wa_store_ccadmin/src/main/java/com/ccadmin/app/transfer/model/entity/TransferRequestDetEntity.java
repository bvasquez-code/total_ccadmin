package com.ccadmin.app.transfer.model.entity;

import com.ccadmin.app.product.model.entity.ProductEntity;
import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import com.ccadmin.app.transfer.exception.TransferException;
import com.ccadmin.app.transfer.model.entity.id.TransferRequestDetId;
import jakarta.persistence.*;

import java.io.Serializable;

@Entity
@Table(name = "transfer_request_det")
@IdClass(TransferRequestDetId.class)
public class TransferRequestDetEntity extends AuditTableEntity implements Serializable {

    @Id
    public String TransferReqCod;
    public String ProductCod;
    public String Variant;
    @Id
    public int ItemNumber;
    public String TypeOperation;
    public String WarehouseCodOrigin;
    public String WarehouseCodDest;
    public int NumUnit;
    public int NumUnitDispatch;
    public int NumUnitReception;
    public String LotNumber;
    public java.util.Date ExpirationDate;

    @Transient
    public ProductEntity Product;

    public TransferRequestDetEntity() {
    }

    public TransferRequestDetEntity validate() throws TransferException {
        if (this.TransferReqCod == null || this.TransferReqCod.trim().isEmpty()) {
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
        return this;
    }

    @Override
    public TransferRequestDetEntity session(String userCod) {
        this.addSession(userCod);
        return this;
    }
}
