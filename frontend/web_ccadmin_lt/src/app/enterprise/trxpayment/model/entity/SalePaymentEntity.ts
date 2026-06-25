import { AuditTableEntity } from "src/app/enterprise/shared/model/entity/AuditTableEntity";
import { TrxPaymentEntity } from "./TrxPaymentEntity";

export class SalePaymentEntity extends AuditTableEntity {

    public PaymentNumber: number;
    public SaleCod: string;
    public TrxPaymentId: number;
    public CurrencyCod: string;
    public CurrencyCodSys: string;
    public NumExchangevalue: number;
    public NumAmountPaid: number;
    public NumAmountPaidOrigin: number;
    public NumAmountReturned: number;
    public TrxPayment : TrxPaymentEntity;

    constructor() {
        super();
        this.PaymentNumber = 0;
        this.SaleCod = '';
        this.TrxPaymentId = 0;
        this.CurrencyCod = '';
        this.CurrencyCodSys = '';
        this.NumExchangevalue = 0;
        this.NumAmountPaid = 0;
        this.NumAmountPaidOrigin = 0;
        this.NumAmountReturned = 0;
        this.TrxPayment = new TrxPaymentEntity();
    }
}
