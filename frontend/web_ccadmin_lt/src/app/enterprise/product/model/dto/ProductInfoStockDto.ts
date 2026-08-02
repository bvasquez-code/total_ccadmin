import { ProductEntity } from '../entity/ProductEntity';
import { ProductInfoEntity } from '../entity/ProductInfoEntity';

export class ProductInfoStockDto {
    public productInfo: ProductInfoEntity = new ProductInfoEntity();
    public product: ProductEntity = new ProductEntity();
}
