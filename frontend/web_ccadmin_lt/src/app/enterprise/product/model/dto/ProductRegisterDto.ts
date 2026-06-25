import { ProductBarcodeEntity } from "../entity/ProductBarcodeEntity";
import { ProductConfigEntity } from "../entity/ProductConfigEntity";
import { ProductEntity } from "../entity/ProductEntity";
import { ProductPictureEntity } from "../entity/ProductPictureEntity";

export class ProductRegisterDto
{
    public product : ProductEntity = new ProductEntity();
    public config : ProductConfigEntity = new ProductConfigEntity();
    public pictureList : ProductPictureEntity[] = [];
    public productBarcode : ProductBarcodeEntity = new ProductBarcodeEntity();

    constructor()
    {
        
    }

}