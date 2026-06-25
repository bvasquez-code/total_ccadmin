import { ProductRegisterDto } from "./ProductRegisterDto";

export class ProductRegisterMassiveDto {

    public productList: ProductRegisterDto[];

    constructor() {
        this.productList = [];
    }
}