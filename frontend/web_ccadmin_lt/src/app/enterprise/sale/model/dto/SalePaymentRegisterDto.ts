export class SalePaymentRegisterDto {

    public SaleCod : string;
    public TrxPaymentId : number;
    public DocumentType : string;
    public CounterfoilCod : string;

    constructor(){
        this.SaleCod = "";
        this.TrxPaymentId = 0;
        this.DocumentType = "";
        this.CounterfoilCod = "";
    }
}
