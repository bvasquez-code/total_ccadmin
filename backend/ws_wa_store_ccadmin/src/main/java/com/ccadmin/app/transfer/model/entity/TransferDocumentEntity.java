package com.ccadmin.app.transfer.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import com.ccadmin.app.transfer.exception.TransferException;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "transfer_document")
public class TransferDocumentEntity extends AuditTableEntity implements Serializable {

    @Id
    public String DocumentCod;
    public String TypeOperation;
    public String CounterfoilCod;
    public String TransferCod;
    public String DocumentRole;
    public String ReasonTransferCod;
    public String ReasonTransferDesc;
    public String TransportModeCod;
    public String DepartureUbigeo;
    public String DepartureAddress;
    public String ArrivalUbigeo;
    public String ArrivalAddress;
    public BigDecimal TotalWeightKg;
    public Integer NumPackages;
    public String CarrierRuc;
    public String CarrierName;
    public String VehiclePlate;
    public String DriverDocType;
    public String DriverDocNumber;
    public String DriverLicenseNumber;
    public String SunatStatus;
    public String SunatTicket;
    public String CdrCode;
    public String CdrDescription;
    public String XmlHash;

    public TransferDocumentEntity() {
    }

    public TransferDocumentEntity validate() throws TransferException {
        if (this.DocumentCod == null || this.DocumentCod.trim().isEmpty()) {
            throw new TransferException("Código de documento es obligatorio");
        }
        if (this.CounterfoilCod == null || this.CounterfoilCod.trim().isEmpty()) {
            throw new TransferException("Código de talonario es obligatorio");
        }
        if (this.TransferCod == null || this.TransferCod.trim().isEmpty()) {
            throw new TransferException("Código de transferencia es obligatorio");
        }
        return this;
    }

    @Override
    public TransferDocumentEntity session(String userCod) {
        this.addSession(userCod);
        return this;
    }
}
