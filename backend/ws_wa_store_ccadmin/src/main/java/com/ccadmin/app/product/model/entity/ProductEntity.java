package com.ccadmin.app.product.model.entity;

import com.ccadmin.app.product.exception.ProductBuildException;
import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import com.ccadmin.app.shared.util.ValidationBasicTypeDataUtil;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@Table( name = "product")
public class ProductEntity  extends AuditTableEntity implements Serializable  {

    @Id
    public String ProductCod;
    public String CategoryCod;
    public String BrandCod;
    public String ProductName;
    public String ProductDesc;

    public ProductEntity(){

    }

    public ProductEntity validate(){
        if(this.ProductCod==null || this.ProductCod.isEmpty()){
            throw new ProductBuildException("Código de producto no puede ser vació");
        }
        if(this.CategoryCod==null || this.CategoryCod.isEmpty()){
            throw new ProductBuildException("Código de categoría no puede ser vació");
        }
        if(this.BrandCod==null || this.BrandCod.isEmpty()){
            throw new ProductBuildException("Código de marca no puede ser vació");
        }
        if(this.ProductName==null || this.ProductName.isEmpty()){
            throw new ProductBuildException("Nombre de producto no puede ser vació");
        }
        if(!ValidationBasicTypeDataUtil.validateText(this.ProductCod,20)){
            throw new ProductBuildException("Código de producto no es valida solo puede ser alfanumérico y como máximo 20 caracteres");
        }
        return this;
    }

    @Override
    public ProductEntity session(String userCod) {
        this.addSession(userCod);
        return this;
    }

    @Override
    public String toString() {
        return "ProductEntity{" +
                "ProductCod='" + ProductCod + '\'' +
                ", CategoryCod='" + CategoryCod + '\'' +
                ", BrandCod='" + BrandCod + '\'' +
                ", ProductName='" + ProductName + '\'' +
                ", ProductDesc='" + ProductDesc + '\'' +
                ", CreationUser='" + CreationUser + '\'' +
                ", CreationDate=" + CreationDate +
                ", ModifyUser='" + ModifyUser + '\'' +
                ", ModifyDate=" + ModifyDate +
                ", Status='" + Status + '\'' +
                '}';
    }
}
