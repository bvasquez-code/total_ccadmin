export class SaleDeliveryOrderDto {
  public SaleCod: string = '';
  public PresaleCod: string = '';
  public StoreCod: string = '';
  public StoreName: string = '';
  public CreationDate: string = '';
  public NumTotalPrice: number = 0;
  public NumTotalPaid: number = 0;
  public PaymentCount: number = 0;
  public CurrencyCod: string = '';
  public SaleStatus: string = '';
  public IsPaid: string = '';
  public DeliveryTypeCod: string = '';
  public DeliveryTypeName: string = '';
  public DeliveryStatus: string = '';
  public Address: string = '';
  public ScheduledFrom: string = '';
  public ScheduledTo: string = '';
  public TrackingNumber: string = '';
  public CanResumePayment: boolean = false;
  public OrderToken: string = '';
}
