package com.ccadmin.app.product.model.entity;

import com.ccadmin.app.product.model.entity.id.ProductPictureID;
import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import com.ccadmin.app.system.model.entity.AppFileEntity;
import jakarta.persistence.*;

import java.io.Serializable;

@Entity
@Table(name = "product_picture")
@IdClass(ProductPictureID.class)
public class ProductPictureEntity extends AuditTableEntity implements Serializable {

    @Id
    public String ProductCod;
    @Id
    public String FileCod;
    public String IsPrincipal;

    @Transient
    public AppFileEntity appFile;

}
