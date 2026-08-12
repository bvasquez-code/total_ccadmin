import { TrxPaymentEntity } from '../entity/TrxPaymentEntity';
import { TrxPaymentDocumentEntity } from '../entity/TrxPaymentDocumentEntity';

export class SalePaymentDeliveryRegisterDto {
  public OrderToken: string = '';
  public TrxPayment: TrxPaymentEntity = new TrxPaymentEntity();
  public DocumentList: TrxPaymentDocumentEntity[] = [];
}
