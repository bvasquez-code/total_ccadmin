import { ProductSearchEntity } from '../../../catalog/model/entity/ProductSearchEntity';
import { ProductInfoDto } from '../../../catalog/model/dto/ProductInfoDto';

export class CartItemDto {
  public ProductCod: string = '';
  public StoreCod: string = '';
  public ProductName: string = '';
  public ProductDesc: string = '';
  public FileRoute: string = '';
  public CurrencyCod: string = '';
  public CurrencySymbol: string = '';
  public ProductUnitName: string = 'NIU';
  public ProductUnitFactor: number = 1;
  public UnitPrice: number = 0;
  public Quantity: number = 1;
  public AvailableQuantity: number = 0;
  public IsDigital: string = 'N';
  public ProductInfo: ProductInfoDto = new ProductInfoDto();

  public static fromProduct(product: ProductSearchEntity): CartItemDto {
    const item = new CartItemDto();
    const factor = Math.max(1, Number(product.ProductUnitFactor || 1));
    item.ProductCod = product.ProductCod;
    item.StoreCod = product.StoreCod;
    item.ProductName = product.ProductName;
    item.ProductDesc = product.ProductDesc;
    item.FileRoute = product.FileRoute;
    item.CurrencyCod = product.CurrencyCod;
    item.CurrencySymbol = product.CurrencySymbol;
    item.ProductUnitName = product.ProductUnitName || 'NIU';
    item.ProductUnitFactor = factor;
    item.UnitPrice = CartItemDto.money(Number(product.NumPrice || 0) * factor);
    item.IsDigital = product.IsDigital || 'N';
    item.ProductInfo = ProductInfoDto.fromProductSearch(product);
    item.AvailableQuantity = item.IsDigital === 'S'
      ? Number.MAX_SAFE_INTEGER
      : Math.max(0, Math.floor(Number(product.NumPhysicalStock || 0) / factor));
    return item;
  }

  private static money(value: number): number {
    return Math.round(value * 100) / 100;
  }
}
