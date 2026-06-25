package com.ccadmin.app.system.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@Table( name = "counterfoil" )
public class CounterfoilEntity extends AuditTableEntity implements Serializable {

    @Id
    public String CounterfoilCod;
    public String DocumentType;
    public String Series;
    public int Correlative;
    public String IsAutomatic;
    public String GroupDocument;


    public CounterfoilEntity validate() {
        if (CounterfoilCod == null || CounterfoilCod.isBlank())
            throw new IllegalArgumentException("CounterfoilCod requerido");
        if (DocumentType == null || DocumentType.isBlank())
            throw new IllegalArgumentException("DocumentType requerido");
        if (Series == null || Series.isBlank())
            throw new IllegalArgumentException("Series requerida");
        if (Correlative < 0)
            throw new IllegalArgumentException("Correlative inválido");
        if (IsAutomatic == null || !"SN".contains(IsAutomatic))
            throw new IllegalArgumentException("IsAutomatic debe ser 'S' o 'N'");
        return this;
    }

    @Override
    public CounterfoilEntity session(String userCod) { this.addSession(userCod); return this; }

}
