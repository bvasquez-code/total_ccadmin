package com.ccadmin.app.sale.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table( name = "period" )
public class PeriodEntity extends AuditTableEntity implements Serializable {

    @Id
    public int PeriodId;
    public String PeriodCod;
    public String PeriodDesc;
    public Date StartDate;
    public Date EndDate;
}
