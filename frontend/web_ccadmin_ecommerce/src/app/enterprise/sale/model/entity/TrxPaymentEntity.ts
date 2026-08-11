import { AuditTableEntity } from '../../../shared/model/entity/AuditTableEntity';

export class TrxPaymentEntity extends AuditTableEntity {
  public TrxPaymentId: number | null = null;
  public CashSessionID: number | null = null;
  public PaymentMethodCod: string = '';
  public PaymentPlatform: string = '';
  public CardNumber: string = '';
  public CardHolderName: string = '';
  public CardExpirationDate: Date | null = null;
  public CardCVV: string = '';
  public TransactionId: string | null = null;
  public PaymentStatus: string = '';
  public CurrencyCod: string = '';
  public CurrencyCodSys: string = '';
  public NumExchangevalue: number = 0;
  public AmountPaid: number = 0;
  public AmountReturned: number = 0;
  public TypeMovement: string = 'I';
  public ReversalOfTrxPaymentId: number | null = null;
}
