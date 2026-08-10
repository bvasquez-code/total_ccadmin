package com.ccadmin.app.sale.model.entity;

import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "virtual_cart")
public class VirtualCartEntity extends AuditTableEntity implements Serializable {

    @Id
    public String CartCod;
    public String ClientCod;
    public String StoreCod;
    public String PresaleCod;
    public String SaleCod;
    @Column(columnDefinition = "json")
    public String CartData;
    public String CartStatus = SaleConstants.CART_STATUS_ACTIVE;
    public Date ExpiresDate;

    public VirtualCartEntity() {
    }
}
