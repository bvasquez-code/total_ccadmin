export class StoreLocationRequestDto {
  public Latitude: number | null = null;
  public Longitude: number | null = null;
  public UbigeoCod: string = '';
  public Address: string = '';
  public IsManual: string = 'N';
}
