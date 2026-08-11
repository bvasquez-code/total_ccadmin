import { ProductInfoDto } from '../../../catalog/model/dto/ProductInfoDto';
import { AuditTableEntity } from '../../../shared/model/entity/AuditTableEntity';

export class PresaleDetEntity extends AuditTableEntity {
  public PresaleCod: string = '';
  public ItemNumber: number = 0;
  public ProductCod: string = '';
  public Variant: string = '0000';
  public NumUnit: number = 0;
  public NumUnitPrice: number = 0;
  public NumDiscount: number = 0;
  public NumUnitPriceSale: number = 0;
  public NumTotalPrice: number = 0;
  public ProductUnitName: string = 'NIU';
  public ProductUnitFactor: number = 1;
  public IsDigital: string = 'N';
  public LotNumber: string = '';
  public ExpirationDate: Date | null = null;
  public ProductInfo: ProductInfoDto = new ProductInfoDto();

  public recalculate(): void {
    this.NumUnitPrice = PresaleDetEntity.money(this.NumUnitPrice);
    this.NumDiscount = PresaleDetEntity.money(this.NumDiscount);
    this.NumUnitPriceSale = PresaleDetEntity.money(this.NumUnitPrice - this.NumDiscount);
    this.NumTotalPrice = PresaleDetEntity.money(this.NumUnitPriceSale * this.NumUnit);
  }

  private static money(value: number): number {
    return Math.round((Number(value || 0) + Number.EPSILON) * 100) / 100;
  }
}
