import { TrxPaymentEntity } from "../entity/TrxPaymentEntity";

export class TrxPaymentComponenRequestDto {

    public InputOutstandingBalance : number = 0;
    public TrxPaymentList : TrxPaymentEntity[] = [];
    public InputTypeMovement : string = 'I';
    public InputReversalAmount : number = 0;
    public TrxPaymentReversalList : TrxPaymentEntity[] = [];

    constructor(){
        
    }
}
