import { BusinessConfigEntity } from "src/app/enterprise/shared/model/entity/BusinessConfigEntity";
import { KardexEntity } from "../entity/KardexEntity";
import { ProductEntity } from "../entity/ProductEntity";

export class KardexDto {

    public kardex: KardexEntity;
    public product: ProductEntity;
    public dataTransaction: any; // Usamos 'any' ya que en el original Java era 'Object'
    public dataTypeOperation : BusinessConfigEntity;

    constructor() {
        this.kardex = new KardexEntity();
        this.product = new ProductEntity();
        this.dataTransaction = {};
        this.dataTypeOperation = new BusinessConfigEntity();
    }
}
