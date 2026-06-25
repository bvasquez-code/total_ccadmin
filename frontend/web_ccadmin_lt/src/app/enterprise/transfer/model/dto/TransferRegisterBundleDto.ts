import { TransferDetEntity } from "../entity/TransferDetEntity";
import { TransferDocumentEntity } from "../entity/TransferDocumentEntity";
import { TransferHeadEntity } from "../entity/TransferHeadEntity";

export class TransferRegisterBundleDto {
    public transferHead: TransferHeadEntity;
    public transferDetList: TransferDetEntity[];
    public transferDocument: TransferDocumentEntity;
    public allowPartial: boolean;

    constructor() {
        this.transferHead = new TransferHeadEntity();
        this.transferDetList = [];
        this.transferDocument = new TransferDocumentEntity();
        this.allowPartial = false;
    }
}
