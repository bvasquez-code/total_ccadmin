import { TransferDetEntity } from "../entity/TransferDetEntity";
import { TransferDocumentEntity } from "../entity/TransferDocumentEntity";
import { TransferHeadEntity } from "../entity/TransferHeadEntity";

export class TransferRequestDetailDto {
    public transferHeadRequest: TransferHeadEntity;
    public transferHead: TransferHeadEntity;
    public transferDetRequestList: TransferDetEntity[];
    public transferDetList: TransferDetEntity[];
    public transferDocumentList: TransferDocumentEntity[];

    constructor() {
        this.transferHeadRequest = new TransferHeadEntity();
        this.transferHead = new TransferHeadEntity();
        this.transferDetRequestList = [];
        this.transferDetList = [];
        this.transferDocumentList = [];
    }
}
