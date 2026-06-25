import { CategoryEntity } from "../entity/CategoryEntity";

export class CategoryRegisterMassiveDto {

    public categoryList: CategoryEntity[];

    constructor() {
        this.categoryList = [];
    }
}