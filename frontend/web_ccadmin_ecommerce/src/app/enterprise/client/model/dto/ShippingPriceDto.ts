import { DeliveryCoverageDto } from './DeliveryCoverageDto';
import { ShippingScheduleDto } from './ShippingScheduleDto';

export class ShippingPriceRequestDto {
  public StoreCod: string = '';
  public DeliveryTypeCod: string = '';
  public Latitude: number | null = null;
  public Longitude: number | null = null;
  public CountryCod: string = '';
  public UbigeoCod: string = '';
}

export class ShippingPriceDto {
  public DeliveryTypeCod: string = '';
  public ScheduleType: string = '';
  public ShippingConfigCod: string = '';
  public ProductCod: string = '';
  public Description: string = '';
  public DistanceKm: number = 0;
  public PriceBase: number = 0;
  public PricePerKm: number = 0;
  public Amount: number = 0;
  public Coverage: DeliveryCoverageDto = new DeliveryCoverageDto();
  public Schedule: ShippingScheduleDto | null = null;
}
