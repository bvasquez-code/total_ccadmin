export class ShippingScheduleTimeSlotDto {
  public Label: string = '';
  public StartTime: string = '';
  public EndTime: string = '';
  public ScheduledFrom: string = '';
  public ScheduledTo: string = '';
}

export class ShippingScheduleDateDto {
  public Date: string = '';
  public Label: string = '';
  public ScheduledFrom: string = '';
  public ScheduledTo: string = '';
  public TimeSlotList: ShippingScheduleTimeSlotDto[] = [];
}

export class ShippingScheduleDto {
  public ScheduleType: string = '';
  public ScheduleName: string = '';
  public UseTimeSlot: string = 'N';
  public DateList: ShippingScheduleDateDto[] = [];
}
