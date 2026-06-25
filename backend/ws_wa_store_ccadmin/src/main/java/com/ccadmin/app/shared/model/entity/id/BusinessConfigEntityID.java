package com.ccadmin.app.shared.model.entity.id;


import java.io.Serializable;

public class BusinessConfigEntityID implements Serializable {

    public String GroupCod;
    public Integer ConfigCorr;

    public BusinessConfigEntityID(){

    }
    public BusinessConfigEntityID(String GroupCod,Integer ConfigCorr){
        this.GroupCod = GroupCod;
        this.ConfigCorr = ConfigCorr;
    }
}
