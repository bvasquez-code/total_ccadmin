import { PresaleHeadEntity } from "../entity/PresaleHeadEntity";
import { SaleDetailDto } from "./SaleDetailDto";

export class PresaleCancellationDetailDto {
    public Headboard: PresaleHeadEntity = new PresaleHeadEntity();
    public SaleDetail: SaleDetailDto | null = null;
    public HasStockReservation: boolean = false;
    public PendingPaymentAmount: number = 0;
}
