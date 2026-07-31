import { SalePickingLineDto } from './SalePickingLineDto';

export class SalePickingConfirmDto {
    public SaleCod: string = "";
    public DetailList: SalePickingLineDto[] = [];
}
