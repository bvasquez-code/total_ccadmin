export class CreditNoteReturnPaymentRegisterDto {

    public CreditNoteCod : string;
    public TrxPaymentId : number;

    constructor(){
        this.CreditNoteCod = "";
        this.TrxPaymentId = 0;
    }
}
