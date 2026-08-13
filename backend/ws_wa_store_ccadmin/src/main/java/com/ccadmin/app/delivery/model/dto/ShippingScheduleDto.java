package com.ccadmin.app.delivery.model.dto;

import java.util.ArrayList;
import java.util.List;

public class ShippingScheduleDto {

    public String ScheduleType;
    public String ScheduleName;
    public String UseTimeSlot;
    public List<ShippingScheduleDateDto> DateList = new ArrayList<>();
}
