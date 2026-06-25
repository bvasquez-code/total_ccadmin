package com.ccadmin.app.shared.model.entity;

import com.ccadmin.app.system.utility.StringUtil;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@Table(name = "business_config_group")
public class BusinessConfigGroupEntity extends AuditTableEntity implements Serializable {

    public Integer GroupId;
    @Id
    public String GroupCod;
    public String GroupIdName;
    public String GroupIdKey;
    public String GroupCodName;
    public String GroupCodKey;
    public String ConfigCorrName;
    public String ConfigCorrKey;
    public String ConfigCodName;
    public String ConfigCodKey;
    public String ConfigValName;
    public String ConfigValKey;
    public String ConfigNameName;
    public String ConfigNameKey;
    public String ConfigDescName;
    public String ConfigDescKey;
    public String Str1ConfigName;
    public String Str1ConfigKey;
    public String Str2ConfigName;
    public String Str2ConfigKey;
    public String Str3ConfigName;
    public String Str3ConfigKey;
    public String Str4ConfigName;
    public String Str4ConfigKey;
    public String Num1ConfigName;
    public String Num1ConfigKey;
    public String Num2ConfigName;
    public String Num2ConfigKey;
    public String Num3ConfigName;
    public String Num3ConfigKey;
    public String Num4ConfigName;
    public String Num4ConfigKey;
    public String Dcm1ConfigName;
    public String Dcm1ConfigKey;
    public String Dcm2ConfigName;
    public String Dcm2ConfigKey;
    public String Dcm3ConfigName;
    public String Dcm3ConfigKey;
    public String Dcm4ConfigName;
    public String Dcm4ConfigKey;
    public String Sta1ConfigName;
    public String Sta1ConfigKey;
    public String Sta2ConfigName;
    public String Sta2ConfigKey;
    public String Sta3ConfigName;
    public String Sta3ConfigKey;
    public String Sta4ConfigName;
    public String Sta4ConfigKey;
    public String CreationUserName;
    public String CreationUserKey;
    public String CreationDateName;
    public String CreationDateKey;
    public String ModifyUserName;
    public String ModifyUserKey;
    public String ModifyDateName;
    public String ModifyDateKey;
    public String StatusName;
    public String StatusKey;
    public String GroupName;
    public String GroupDesc;

    public BusinessConfigGroupEntity validate() {
        if (this.GroupId == null) {
            throw new RuntimeException("GroupId es requerido.");
        }
        if (StringUtil.isBlank(this.GroupCod)) {
            throw new RuntimeException("GroupCod es requerido.");
        }
        this.GroupCod = this.GroupCod.trim();
        return this;
    }
}
