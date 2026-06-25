package com.ccadmin.app.transfer.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import com.ccadmin.app.transfer.exception.TransferException;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "transfer_head")
public class TransferHeadEntity extends AuditTableEntity implements Serializable {

    @Id
    public String TransferCod;
    public String TypeOperation;
    public String StoreCodOrigin;
    public String StoreCodDest;
    public String StoreCodRequestedBy;
    public String TransferStatus;
    public String ReceiveStatus;
    public Date DispatchDate;
    public Date ArrivalDate;
    public String UserOriginConfirm;
    public Date DateOriginConfirm;
    public String UserDestConfirm;
    public Date DateDestConfirm;
    public String Observation;

    public TransferHeadEntity() {
    }

    public TransferHeadEntity validate() throws TransferException {
        if (this.TransferCod == null || this.TransferCod.trim().isEmpty()) {
            throw new TransferException("Código de transferencia está vacío");
        }
        if (this.TypeOperation == null || this.TypeOperation.trim().isEmpty()) {
            throw new TransferException("Tipo de operación es obligatorio");
        }
        if (this.StoreCodOrigin == null || this.StoreCodOrigin.trim().isEmpty()) {
            throw new TransferException("Código de local origen es obligatorio");
        }
        if (this.StoreCodDest == null || this.StoreCodDest.trim().isEmpty()) {
            throw new TransferException("Código de local destino es obligatorio");
        }
        if (this.StoreCodOrigin.equals(this.StoreCodDest)) {
            throw new TransferException("El local origen y destino no pueden ser iguales");
        }
        return this;
    }

    @Override
    public TransferHeadEntity session(String userCod) {
        this.addSession(userCod);
        return this;
    }
}
