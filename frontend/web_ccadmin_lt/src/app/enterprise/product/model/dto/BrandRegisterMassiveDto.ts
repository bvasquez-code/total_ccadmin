import { BrandEntity } from "../entity/BrandEntity";

export class BrandRegisterMassiveDto {
    public brandList: BrandEntity[];

    constructor() {
        this.brandList = [];
    }
}