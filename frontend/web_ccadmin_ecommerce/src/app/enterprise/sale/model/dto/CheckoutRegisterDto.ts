import { CheckoutDeliveryDto } from './CheckoutDeliveryDto';
import { PresaleRegisterDto } from './PresaleRegisterDto';

export class CheckoutRegisterDto extends PresaleRegisterDto {
  public Delivery: CheckoutDeliveryDto = new CheckoutDeliveryDto();
}
