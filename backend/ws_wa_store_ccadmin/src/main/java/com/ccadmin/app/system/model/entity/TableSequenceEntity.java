package com.ccadmin.app.system.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.io.Serializable;

@Entity
@Table(name = "table_sequence")
public class TableSequenceEntity implements Serializable {

    @Id
    public Long SequenceTrx;
    public String Prefix;
    public String SequenceTableType;
    public Integer length;
    public String UsePrefix;

    @Transient
    public Long OriginalSequenceTrx;

    public TableSequenceEntity validate() {
        if (SequenceTrx == null || SequenceTrx < 0) {
            throw new IllegalArgumentException("SequenceTrx no debe ser negativo");
        }
        if (Prefix == null || Prefix.isBlank()) {
            throw new IllegalArgumentException("Prefix requerido");
        }
        if (Prefix.length() > 2) {
            throw new IllegalArgumentException("Prefix solo puede tener 2 caracteres");
        }
        if (SequenceTableType == null || SequenceTableType.isBlank()) {
            throw new IllegalArgumentException("SequenceTableType requerido");
        }
        if (SequenceTableType.length() > 32) {
            throw new IllegalArgumentException("SequenceTableType solo puede tener 32 caracteres");
        }
        if (length == null || length <= 0) {
            throw new IllegalArgumentException("length debe ser mayor a cero");
        }
        if (UsePrefix == null || UsePrefix.isBlank()) {
            throw new IllegalArgumentException("UsePrefix requerido");
        }
        if (!UsePrefix.equals("S") && !UsePrefix.equals("N")) {
            throw new IllegalArgumentException("UsePrefix debe ser S o N");
        }
        if (UsePrefix.equals("S") && Prefix.length() >= length) {
            throw new IllegalArgumentException("length debe ser mayor que la longitud del prefijo");
        }
        return this;
    }
}
