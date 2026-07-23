package com.ccadmin.app.inventory.model.dto;

import java.util.Date;

public class StockResolutionLineDto {
    public Integer ItemNumber;
    public Integer NumUnit;
    public Integer ResolutionVersion;
    public String ResolutionType;
    public String ResolutionReasonCode;
    public String Observation;
    public Date NextReviewDate;
}
