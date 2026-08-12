package com.ccadmin.app.shared.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@Table(name = "ubigeo_department")
public class UbigeoDepartmentEntity implements Serializable {

    @Id
    public String DepartmentCod;
    public String Name;

    public UbigeoDepartmentEntity() {
    }
}
