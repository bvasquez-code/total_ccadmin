import { Component } from '@angular/core';

@Component({
  selector: 'app-storefront-footer',
  templateUrl: './storefront-footer.component.html',
  styleUrls: ['./storefront-footer.component.css']
})
export class StorefrontFooterComponent {
  public readonly CurrentYear = new Date().getFullYear();
}
