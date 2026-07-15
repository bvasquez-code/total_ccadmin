import { TransferRequestDetEntity } from "../entity/TransferRequestDetEntity";
import { TransferRequestHeadEntity } from "../entity/TransferRequestHeadEntity";

export class TransferRequestDetSaveDto {
    public transferHead: TransferRequestHeadEntity;
    public transferDet: TransferRequestDetEntity;

    constructor() {
        this.transferHead = new TransferRequestHeadEntity();
        this.transferDet = new TransferRequestDetEntity();
    }
}
