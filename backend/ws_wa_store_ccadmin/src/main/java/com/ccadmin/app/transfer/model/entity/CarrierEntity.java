package com.ccadmin.app.transfer.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@Table(name = "carrier")
public class CarrierEntity extends AuditTableEntity implements Serializable {

    @Id
    public String CarrierCod;
    public String CarrierRuc;
    public String CarrierName;
    public String VehiclePlate;
    public String DriverDocType;
    public String DriverDocNumber;
    public String DriverLicenseNumber;

    public CarrierEntity validate() {
        if (CarrierCod == null || CarrierCod.isBlank()) {
            throw new IllegalArgumentException("CarrierCod requerido");
        }
        if (CarrierCod.length() > 16) {
            throw new IllegalArgumentException("CarrierCod no debe superar 16 caracteres");
        }
        if (CarrierRuc != null && CarrierRuc.length() > 11) {
            throw new IllegalArgumentException("CarrierRuc no debe superar 11 caracteres");
        }
        if (CarrierName != null && CarrierName.length() > 128) {
            throw new IllegalArgumentException("CarrierName no debe superar 128 caracteres");
        }
        if (VehiclePlate != null && VehiclePlate.length() > 16) {
            throw new IllegalArgumentException("VehiclePlate no debe superar 16 caracteres");
        }
        if (DriverDocType != null && DriverDocType.length() > 2) {
            throw new IllegalArgumentException("DriverDocType no debe superar 2 caracteres");
        }
        if (DriverDocNumber != null && DriverDocNumber.length() > 16) {
            throw new IllegalArgumentException("DriverDocNumber no debe superar 16 caracteres");
        }
        if (DriverLicenseNumber != null && DriverLicenseNumber.length() > 16) {
            throw new IllegalArgumentException("DriverLicenseNumber no debe superar 16 caracteres");
        }
        return this;
    }

    @Override
    public CarrierEntity session(String userCod) {
        this.addSession(userCod);
        return this;
    }
}
