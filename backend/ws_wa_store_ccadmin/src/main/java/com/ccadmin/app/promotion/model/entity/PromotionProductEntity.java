package com.ccadmin.app.promotion.model.entity;

import com.ccadmin.app.promotion.model.entity.id.PromotionProductID;
import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "promotion_product")
@IdClass(PromotionProductID.class)
public class PromotionProductEntity extends AuditTableEntity implements Serializable {

    @Id
    public String PromotionCod;
    @Id
    public String ProductCod;
    @Id
    public int PositionPackage;
    @Id
    public int Package;
    public String TypeDiscount;
    public BigDecimal DiscountAmount;

    public PromotionProductEntity(){

    }

    public PromotionProductEntity(String promotionCod, String productCod, int positionPackage, String typeDiscount, BigDecimal discountAmount) {
        PromotionCod = promotionCod;
        ProductCod = productCod;
        PositionPackage = positionPackage;
        TypeDiscount = typeDiscount;
        DiscountAmount = discountAmount;
    }

    public PromotionProductEntity buildSimple(String promotionCod, String productCod, String typeDiscount, BigDecimal discountAmount){
        PromotionCod = promotionCod;
        ProductCod = productCod;
        PositionPackage = 1;
        Package = 1;
        TypeDiscount = typeDiscount;
        DiscountAmount = discountAmount;
        return this;
    }

    public PromotionProductEntity setPackage(int Package,int PositionPackage){
        this.PositionPackage = PositionPackage;
        this.Package = Package;
        return this;
    }

    public PromotionProductEntity setDiscount(BigDecimal DiscountAmount){
        this.TypeDiscount = "P";
        this.DiscountAmount = DiscountAmount;
        return this;
    }
}
