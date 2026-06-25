package com.ccadmin.app.shared.model.entity;

import com.ccadmin.app.shared.model.entity.id.BusinessConfigEntityID;
import com.ccadmin.app.system.utility.StringUtil;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table( name = "business_config")
@IdClass(BusinessConfigEntityID.class)
public class BusinessConfigEntity extends AuditTableEntity implements Serializable {

    public Integer GroupId;
    @Id
    public String GroupCod;
    @Id
    public Integer ConfigCorr;
    public String ConfigCod;
    public String ConfigVal;
    public String ConfigName;
    public String ConfigDesc;
    public String Str1Config;
    public String Str2Config;
    public String Str3Config;
    public String Str4Config;
    public Integer Num1Config;
    public Integer Num2Config;
    public Integer Num3Config;
    public Integer Num4Config;
    public BigDecimal Dcm1Config;
    public BigDecimal Dcm2Config;
    public BigDecimal Dcm3Config;
    public BigDecimal Dcm4Config;
    public String Sta1Config;
    public String Sta2Config;
    public String Sta3Config;
    public String Sta4Config;

    public BusinessConfigEntity validate() {
        if (this.GroupId == null) {
            throw new RuntimeException("GroupId es requerido.");
        }
        if (StringUtil.isBlank(this.GroupCod)) {
            throw new RuntimeException("GroupCod es requerido.");
        }
        if (this.ConfigCorr == null) {
            throw new RuntimeException("ConfigCorr es requerido.");
        }
        if (StringUtil.isBlank(this.ConfigCod)) {
            throw new RuntimeException("ConfigCod es requerido.");
        }
        this.GroupCod = this.GroupCod.trim();
        this.ConfigCod = this.ConfigCod.trim();
        return this;
    }
}
