import { CounterfoilRegisterDto } from "./CounterfoilRegisterDto";

export class CounterfoilRegisterListDto {
    public registerList: CounterfoilRegisterDto[];

    constructor() {
        this.registerList = [];
    }
}
