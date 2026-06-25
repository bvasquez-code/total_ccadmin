import { Pipe, PipeTransform } from '@angular/core';
import { SaleConstants } from '../constants/SaleConstants';

@Pipe({
  name: 'SaleStatusPipe'
})
export class SaleStatusPipePipe implements PipeTransform {

  transform(status: string): string {
    return SaleConstants.getStatusDescription(status);
  }

}
