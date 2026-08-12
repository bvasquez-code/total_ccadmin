package com.ccadmin.app.shared.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@Table(name = "country")
public class CountryEntity extends AuditTableEntity implements Serializable {

    @Id
    public String CountryCod;
    public String CountryIso2;
    public String CountryName;

    public CountryEntity() {
    }
}
