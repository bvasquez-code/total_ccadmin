import { Component, Input } from '@angular/core';


@Component({
  selector: 'app-productinfosalemodal',
  templateUrl: './productinfosalemodal.component.html'
})
export class ProductinfosalemodalComponent {

  @Input() productInfoDtoSelect: any;
  @Input() CurrencySystem: any;
  @Input() NumPhysicalStockTotal: number = 0;

  getTotalProduct(productCod: string): number {
    return 0;
  }

  subtractUnit(productInfo: any, variant: any): void {
    // Implementa la lógica para restar unidades
  }

  addUnit(productInfo: any, variant: any): void {
    // Implementa la lógica para agregar unidades
  }

  HandbookUnit(productInfo: any, variant: any): void {
    // Implementa la lógica para manejar el cambio de unidades
  }

}
