import { AuditTableEntity } from 'src/app/enterprise/shared/model/entity/AuditTableEntity';

export class SaleDeliveryEntity extends AuditTableEntity {
  public SaleCod: string = '';
  public DeliveryTypeCod: string = '';
  public DeliveryStatus: string = '';
  public ClientAddressID: number | null = null;
  public IsThirdParty: string = 'N';
  public Names: string = '';
  public DocumentType: string = '';
  public DocumentNumber: string = '';
  public Phone: string = '';
  public Email: string = '';
  public Address: string = '';
  public GeocodedAddress: string = '';
  public Reference: string = '';
  public CountryCod: string = '';
  public CountryName: string = '';
  public StateName: string = '';
  public CityName: string = '';
  public UbigeoCod: string = '';
  public Latitude: number | null = null;
  public Longitude: number | null = null;
  public Instructions: string = '';
  public EstimatedDistanceKm: number | null = null;
  public ScheduledFrom: Date | null = null;
  public ScheduledTo: Date | null = null;
  public ShippingProviderCod: string = '';
  public TrackingNumber: string = '';
  public AgencyName: string = '';
  public AgencyAddress: string = '';
  public ReadyDate: Date | null = null;
  public DispatchDate: Date | null = null;
  public DeliveredDate: Date | null = null;
  public Commenter: string = '';

  constructor() {
    super();
  }
}
