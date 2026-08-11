import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CartItemDto } from '../../../cart/model/dto/CartItemDto';

@Component({
  selector: 'app-cart-product-modal',
  templateUrl: './cart-product-modal.component.html',
  styleUrls: ['./cart-product-modal.component.css']
})
export class CartProductModalComponent {
  @Input() public IsOpen: boolean = false;
  @Input() public Item: CartItemDto | null = null;

  @Output() public Close = new EventEmitter<void>();
  @Output() public Decrease = new EventEmitter<void>();
  @Output() public Increase = new EventEmitter<void>();
  @Output() public NavigateToCart = new EventEmitter<void>();
}
