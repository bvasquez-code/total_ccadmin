import { ProductSearchEntity } from '../entity/ProductSearchEntity';

export class ProductDeliveryDetailDto {
  public Product: ProductSearchEntity = new ProductSearchEntity();
  public PictureList: ProductDeliveryPictureDto[] = [];
}

export class ProductDeliveryPictureDto {
  public ProductCod: string = '';
  public FileCod: string = '';
  public IsPrincipal: string = 'N';
  public appFile: ProductDeliveryFileDto = new ProductDeliveryFileDto();
}

export class ProductDeliveryFileDto {
  public FileCod: string = '';
  public Name: string = '';
  public Description: string = '';
  public Route: string = '';
  public FileType: string = '';
}
