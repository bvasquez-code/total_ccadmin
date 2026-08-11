import { StoreEntity } from '../entity/StoreEntity';

export class StoreContextDto {
  public Store: StoreEntity = new StoreEntity();
  public Latitude: number | null = null;
  public Longitude: number | null = null;
  public Address: string = '';
  public DistanceKm: number | null = null;
  public AllowsAutomaticDelivery: string = 'N';
  public AllowsScheduledDelivery: string = 'N';
  public AllowsStorePickup: string = 'N';
  public DeliveryMessage: string = '';
}
