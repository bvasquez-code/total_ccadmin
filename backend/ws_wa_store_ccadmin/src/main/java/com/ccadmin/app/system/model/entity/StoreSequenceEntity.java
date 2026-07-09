package com.ccadmin.app.system.model.entity;

import com.ccadmin.app.system.model.entity.id.StoreSequenceID;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@Table(name = "store_sequence")
@IdClass(StoreSequenceID.class)
public class StoreSequenceEntity implements Serializable {

    @Id
    public String StoreCod;
    @Id
    public Integer PeriodId;
    @Id
    public String SequenceTableType;
    public Long SequenceTrx;
    public String Prefix;
    public Integer SequenceLength;

    public StoreSequenceEntity validateForStore() {
        if (StoreCod == null || StoreCod.isBlank()) {
            throw new IllegalArgumentException("StoreCod requerido");
        }
        return this.validateCommon();
    }

    public StoreSequenceEntity validateForAllStores() {
        return this.validateCommon();
    }

    private StoreSequenceEntity validateCommon() {
        if (PeriodId == null || PeriodId <= 0) {
            throw new IllegalArgumentException("PeriodId requerido");
        }
        if (SequenceTableType == null || SequenceTableType.isBlank()) {
            throw new IllegalArgumentException("SequenceTableType requerido");
        }
        if (SequenceTableType.length() > 32) {
            throw new IllegalArgumentException("SequenceTableType solo puede tener 32 caracteres");
        }
        if (Prefix == null || Prefix.isBlank()) {
            throw new IllegalArgumentException("Prefix requerido");
        }
        if (Prefix.length() > 2) {
            throw new IllegalArgumentException("Prefix solo puede tener 2 caracteres");
        }
        if (SequenceTrx == null || SequenceTrx < 0) {
            throw new IllegalArgumentException("SequenceTrx no debe ser negativo");
        }
        if (SequenceLength == null || SequenceLength <= 0) {
            throw new IllegalArgumentException("SequenceLength debe ser mayor a cero");
        }
        return this;
    }
}
