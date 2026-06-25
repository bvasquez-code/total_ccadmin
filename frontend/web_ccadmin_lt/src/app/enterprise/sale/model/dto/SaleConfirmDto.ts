export class SaleConfirmDto{

    public SaleCod : string;
    public DocumentType : string;
    public CounterfoilCod : string;

    constructor(){
        this.SaleCod = "";
        this.DocumentType = "";
        this.CounterfoilCod = "";
    }

}