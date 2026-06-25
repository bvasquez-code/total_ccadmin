import { ProductEntity } from "src/app/enterprise/product/model/entity/ProductEntity";
import { CreditNoteDetEntity } from "../entity/CreditNoteDetEntity";

export class CreditNoteDetDto {

    public CreditNoteDet: CreditNoteDetEntity;
    public Product: ProductEntity;

    constructor() {
        this.CreditNoteDet = new CreditNoteDetEntity();
        this.Product = new ProductEntity();
    }
}
