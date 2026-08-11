import { SaleDetailDto } from './SaleDetailDto';

export class CheckoutConfirmationDto {
  public OrderToken: string = '';
  public SaleDetail: SaleDetailDto | null = null;
}
