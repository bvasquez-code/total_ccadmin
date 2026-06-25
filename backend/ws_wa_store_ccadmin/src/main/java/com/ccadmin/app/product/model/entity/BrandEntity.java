package com.ccadmin.app.product.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@Table( name = "brand")
public class BrandEntity extends AuditTableEntity implements Serializable {

    @Id
    public String BrandCod;
    public String BrandName;

}
