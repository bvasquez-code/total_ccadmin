import { ProductTaxConfigEntity } from "../entity/ProductTaxConfigEntity";

export class ProductTaxConfigRegisterDto {
    public ProductCod: string = "";
    public StoreCod: string = "";
    public TaxConfigList: ProductTaxConfigEntity[] = [];
}
