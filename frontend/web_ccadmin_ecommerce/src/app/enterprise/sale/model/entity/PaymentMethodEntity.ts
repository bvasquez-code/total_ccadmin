import { AuditTableEntity } from '../../../shared/model/entity/AuditTableEntity';

export class PaymentMethodEntity extends AuditTableEntity {
  public PaymentMethodCod: string = '';
  public Name: string = '';
  public Description: string = '';
  public PaymentMethodType: string = '';
  public IsInternalSaleEnabled: string = 'S';
  public IsWebSaleEnabled: string = 'S';
  public IsPaymentProofRequired: string = 'N';
  public FileCod: string = '';
  public Route: string = '';
}
