export class DeliveryCoverageDto {
  public StoreCod: string = '';
  public DeliveryTypeCod: string = '';
  public DistanceKm: number | null = null;
  public MaximumDistanceKm: number | null = null;
  public IsAvailable: string = 'N';
  public Message: string = '';
}
