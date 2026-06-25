import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'FormatoMonedaPeruana'
})
export class FormatoMonedaPeruanaPipe implements PipeTransform {

  transform(value: any, mostrarSimbolo: boolean = true): any {
    if (value == null) {
      return null;
    }
    
    const roundedValue = Math.round(value * 100) / 100;
    
    const parts = roundedValue.toString().split(".");
    parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ",");
    
    if (mostrarSimbolo) {
      return 'S/. ' + parts.join(".");
    } else {
      return parts.join(".");
    }
  }

}
