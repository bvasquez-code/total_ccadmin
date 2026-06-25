package com.ccadmin.app.product.model.entity;

import com.ccadmin.app.product.model.entity.id.ProductRankingID;
import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_ranking")
@IdClass(ProductRankingID.class)
public class ProductRankingEntity extends AuditTableEntity {

    @Id
    public String ProductCod;

    @Id
    public String StoreCod;

    public Integer RankingPoints;

    public ProductRankingEntity(){
        this.RankingPoints = 0;
    }

    public ProductRankingEntity(String productCod, String storeCod, Integer rankingPoints) {
        ProductCod = productCod;
        StoreCod = storeCod;
        RankingPoints = rankingPoints;
    }

    @Override
    public ProductRankingEntity session(String userCod) {
        this.addSession(userCod);
        return this;
    }

    public ProductRankingEntity plusRankingPoints(int plusPoints){
        this.RankingPoints = this.RankingPoints + plusPoints;
        return this;
    }
}
