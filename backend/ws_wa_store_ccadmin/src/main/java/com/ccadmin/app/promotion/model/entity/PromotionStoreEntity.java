package com.ccadmin.app.promotion.model.entity;

import com.ccadmin.app.promotion.model.entity.id.PromotionStoreID;
import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@Table( name = "promotion_store" )
@IdClass(PromotionStoreID.class)
public class PromotionStoreEntity extends AuditTableEntity implements Serializable {

    @Id
    public String PromotionCod;
    @Id
    public String StoreCod;

    public PromotionStoreEntity(String PromotionCod,String StoreCod){
        this.PromotionCod = PromotionCod;
        this.StoreCod = StoreCod;
    }

}
