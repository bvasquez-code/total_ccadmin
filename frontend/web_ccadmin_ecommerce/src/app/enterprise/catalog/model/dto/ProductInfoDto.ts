import { AuditTableEntity } from '../../../shared/model/entity/AuditTableEntity';
import { ProductSearchEntity } from '../entity/ProductSearchEntity';

export class ProductInfoDto {
  public Product: ProductDto = new ProductDto();
  public Config: ProductConfigDto = new ProductConfigDto();
  public VariantList: ProductVariantDto[] = [];
  public InfoList: ProductStockDto[] = [];
  public InfoWarehouseList: ProductStockDto[] = [];
  public Picture: ProductPictureDto = new ProductPictureDto();

  public static fromProductSearch(product: ProductSearchEntity): ProductInfoDto {
    const result = new ProductInfoDto();
    result.Product.ProductCod = product.ProductCod;
    result.Product.CategoryCod = product.CategoryCod;
    result.Product.BrandCod = product.BrandCod;
    result.Product.ProductName = product.ProductName;
    result.Product.ProductDesc = product.ProductDesc;

    result.Config.ProductCod = product.ProductCod;
    result.Config.StoreCod = product.StoreCod;
    result.Config.NumPrice = Number(product.NumPrice || 0);
    result.Config.IsDigital = product.IsDigital || 'N';
    result.Config.ProductUnitName = product.ProductUnitName || 'NIU';
    result.Config.ProductUnitFactor = Math.max(1, Number(product.ProductUnitFactor || 1));

    const variant = new ProductVariantDto();
    variant.ProductCod = product.ProductCod;
    variant.Variant = '0000';
    variant.VariantDesc = 'default';
    result.VariantList = [variant];

    const stock = new ProductStockDto();
    stock.ProductCod = product.ProductCod;
    stock.Variant = '0000';
    stock.StoreCod = product.StoreCod;
    stock.NumDigitalStock = Number(product.NumDigitalStock || 0);
    stock.NumPhysicalStock = Number(product.NumPhysicalStock || 0);
    stock.NumUnavailableStock = Number(product.NumUnavailableStock || 0);
    stock.NumReservedStock = Number(product.NumReservedStock || 0);
    stock.NumTotalStock = Number(product.NumTotalStock || 0);
    result.InfoList = [stock];

    result.Picture.ProductCod = product.ProductCod;
    result.Picture.FileCod = product.FileCod;
    result.Picture.FileRoute = product.FileRoute;
    result.Picture.IsPrincipal = product.FileCod ? 'S' : 'N';
    return result;
  }
}

export class ProductDto extends AuditTableEntity {
  public ProductCod: string = '';
  public CategoryCod: string = '';
  public BrandCod: string = '';
  public ProductName: string = '';
  public ProductDesc: string = '';
}

export class ProductConfigDto extends AuditTableEntity {
  public ProductCod: string = '';
  public StoreCod: string = '';
  public NumPrice: number = 0;
  public IsDigital: string = 'N';
  public IsDiscontable: string = 'N';
  public DiscountType: string = '';
  public NumDiscountMax: number = 0;
  public ProductUnitName: string = 'NIU';
  public ProductUnitFactor: number = 1;
}

export class ProductVariantDto extends AuditTableEntity {
  public ProductCod: string = '';
  public Variant: string = '';
  public VariantDesc: string = '';
}

export class ProductStockDto extends AuditTableEntity {
  public ProductCod: string = '';
  public Variant: string = '';
  public StoreCod: string = '';
  public NumDigitalStock: number = 0;
  public NumPhysicalStock: number = 0;
  public NumUnavailableStock: number = 0;
  public NumReservedStock: number = 0;
  public NumTotalStock: number = 0;
}

export class ProductPictureDto extends AuditTableEntity {
  public ProductCod: string = '';
  public FileCod: string = '';
  public FileRoute: string = '';
  public IsPrincipal: string = 'N';
}
