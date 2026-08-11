export class CheckoutDeliveryDto {
  public DeliveryTypeCod: string = '';
  public ClientAddressID: number | null = null;
  public IsThirdParty: string = 'N';
  public Names: string = '';
  public DocumentType: string = '01';
  public DocumentNumber: string = '';
  public Phone: string = '';
  public Email: string = '';
  public Address: string = '';
  public Reference: string = '';
  public UbigeoCod: string = '';
  public Latitude: number | null = null;
  public Longitude: number | null = null;
  public EstimatedDistanceKm: number | null = null;
  public Instructions: string = '';
  public ScheduledFrom: string = '';
  public ScheduledTo: string = '';
}
