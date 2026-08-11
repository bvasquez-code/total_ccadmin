import { TrxPaymentEntity } from '../entity/TrxPaymentEntity';

export class SalePaymentDeliveryRegisterDto {
  public OrderToken: string = '';
  public TrxPayment: TrxPaymentEntity = new TrxPaymentEntity();
}
