package com.ccadmin.app.delivery.model.dto;

import java.util.ArrayList;
import java.util.List;

public class ShippingScheduleDateDto {

    public String Date;
    public String Label;
    public String ScheduledFrom;
    public String ScheduledTo;
    public List<ShippingScheduleTimeSlotDto> TimeSlotList = new ArrayList<>();
}
