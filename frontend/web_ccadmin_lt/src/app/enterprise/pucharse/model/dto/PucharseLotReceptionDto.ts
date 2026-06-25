export class PucharseLotReceptionDto {

    public NumUnit: number = 0;
    public LotNumber: string = "";
    public ExpirationDate: string = "";

    constructor(init?: Partial<PucharseLotReceptionDto>)
    {
        Object.assign(this, init);
    }
}
