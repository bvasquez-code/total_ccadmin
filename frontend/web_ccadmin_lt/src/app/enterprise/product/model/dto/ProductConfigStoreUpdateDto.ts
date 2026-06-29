import { ProductConfigEntity } from "../entity/ProductConfigEntity";

export class ProductConfigStoreUpdateDto {
    public ProductCod: string = "";
    public StoreCod: string = "";
    public StoreCodList: string[] = [];
    public ApplyAllStores: boolean = false;
    public config: ProductConfigEntity = new ProductConfigEntity();
}
