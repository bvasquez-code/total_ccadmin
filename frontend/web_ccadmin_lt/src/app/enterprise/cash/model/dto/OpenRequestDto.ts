export class OpenRequestDto {
    public CurrencyCod: string;
    public Commenter: string;
    public OpeningFloatAmount: number;

    constructor() {
        this.CurrencyCod = 'PEN';
        this.Commenter = '';
        this.OpeningFloatAmount = 0;
    }
}
