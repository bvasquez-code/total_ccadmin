import { TransferRequestDetEntity } from "../entity/TransferRequestDetEntity";
import { TransferRequestHeadEntity } from "../entity/TransferRequestHeadEntity";
import { TransferRegisterBundleDto } from "./TransferRegisterBundleDto";

export class TransferRequestRegisterBundleDto {
    public transferHead: TransferRequestHeadEntity;
    public transferDetList: TransferRequestDetEntity[];
    public allowPartial: boolean;

    constructor() {
        this.transferHead = new TransferRequestHeadEntity();
        this.transferDetList = [];
        this.allowPartial = false;
    }

    public buildTransferRegister(): TransferRegisterBundleDto {
        const entity = new TransferRegisterBundleDto();
        entity.transferHead = this.transferHead.buildTransferHead();
        entity.transferDetList = this.transferDetList.map(e => e.buildTransferDet());
        entity.allowPartial = this.allowPartial;
        return entity;
    }
}
