import { CartItemDto } from './CartItemDto';

export class ShoppingCartDto {
  public StoreCod: string = '';
  public Items: CartItemDto[] = [];
  public UpdatedAt: string = '';
}
