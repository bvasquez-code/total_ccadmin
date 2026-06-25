package com.ccadmin.app.promotion.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table( name = "promotion" )
public class PromotionEntity extends AuditTableEntity implements Serializable {

    @Id
    public String PromotionCod;
    public String Name;
    public String Description;
    public String DescriptionLong;
    public Date InitialDate;
    public Date FinalDate;
    public Date InitialUseDate;
    public Date FinalUseDate;
    public String TypePromotion;
    public String WayOfUse;

}
