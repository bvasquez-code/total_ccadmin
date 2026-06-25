package com.ccadmin.app.product.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@Table( name = "category")
public class CategoryEntity extends AuditTableEntity implements Serializable {

    @Id
    public String CategoryCod;
    public String CategoryName;
    public String CategoryDadCod;
    public String IsDigital;
    public String IsCategoryDad;

}
