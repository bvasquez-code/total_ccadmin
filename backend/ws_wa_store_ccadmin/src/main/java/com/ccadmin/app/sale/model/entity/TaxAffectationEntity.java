package com.ccadmin.app.sale.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@Table(name = "tax_affectation")
public class TaxAffectationEntity extends AuditTableEntity implements Serializable {

    @Id
    public String TaxAffectationCod;
    public String TaxCod;
    public String Name;
    public String Description;
    public String IsTaxed;

    public TaxAffectationEntity validate() {
        if (TaxAffectationCod == null || TaxAffectationCod.isBlank()) {
            throw new IllegalArgumentException("TaxAffectationCod requerido");
        }
        if (TaxCod == null || TaxCod.isBlank()) {
            throw new IllegalArgumentException("TaxCod requerido");
        }
        if (Name == null || Name.isBlank()) {
            throw new IllegalArgumentException("Nombre de afectacion requerido");
        }
        if (IsTaxed == null || IsTaxed.isBlank()) {
            IsTaxed = "N";
        }
        if (!IsTaxed.equals("S") && !IsTaxed.equals("N")) {
            throw new IllegalArgumentException("Indicador gravado no valido");
        }
        return this;
    }

    @Override
    public TaxAffectationEntity session(String userCod) {
        this.addSession(userCod);
        return this;
    }
}
