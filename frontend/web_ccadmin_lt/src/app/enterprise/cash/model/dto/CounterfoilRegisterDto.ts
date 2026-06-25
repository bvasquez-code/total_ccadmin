import { CounterfoilEntity } from "../entity/CounterfoilEntity";
import { CounterfoilStoreEntity } from "../entity/CounterfoilStoreEntity";

export class CounterfoilRegisterDto {

    public counterfoil: CounterfoilEntity;
    public counterfoilStore: CounterfoilStoreEntity;

    constructor() {
        this.counterfoil = new CounterfoilEntity();
        this.counterfoilStore = new CounterfoilStoreEntity();
    }
}
