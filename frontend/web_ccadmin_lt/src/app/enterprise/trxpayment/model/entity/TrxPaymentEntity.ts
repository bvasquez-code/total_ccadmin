import { AuditTableEntity } from "src/app/enterprise/shared/model/entity/AuditTableEntity";

export class TrxPaymentEntity extends AuditTableEntity {

    public TrxPaymentId: number;
    public PaymentMethodCod: string;
    public PaymentPlatform: string;
    public CardNumber: string;
    public CardHolderName: string;
    public CardExpirationDate: Date;
    public CardCVV: string;
    public TransactionId: string | null;
    public PaymentStatus: string;
    public CurrencyCod: string;
    public CurrencyCodSys: string;
    public NumExchangevalue: number;
    public AmountPaid: number;
    public AmountReturned: number;
    public TypeMovement : string;
    public ReversalOfTrxPaymentId : number | null;

    constructor() {
        super();
        this.TrxPaymentId = 0;
        this.PaymentMethodCod = '';
        this.PaymentPlatform = '';
        this.CardNumber = '';
        this.CardHolderName = '';
        this.CardExpirationDate = new Date();
        this.CardCVV = '';
        this.TransactionId = '';
        this.PaymentStatus = '';
        this.CurrencyCod = '';
        this.CurrencyCodSys = '';
        this.NumExchangevalue = 0;
        this.AmountPaid = 0;
        this.AmountReturned = 0;
        this.TypeMovement = 'I'; // I: Ingreso, E: Egreso
        this.ReversalOfTrxPaymentId = null;
    }
}
