export class OpenRequestDto {
    public RegisterCod: string;
    public StoreCod: string;
    public CurrencyCod: string;
    public Commenter: string;
    public OpeningFloatAmount: number;

    constructor() {
        this.RegisterCod = '';
        this.StoreCod = '';
        this.CurrencyCod = 'PEN';
        this.Commenter = '';
        this.OpeningFloatAmount = 0;
    }
}
