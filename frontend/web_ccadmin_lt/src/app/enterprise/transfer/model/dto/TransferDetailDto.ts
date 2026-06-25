import { TransferDetEntity } from "../entity/TransferDetEntity";
import { TransferDocumentEntity } from "../entity/TransferDocumentEntity";
import { TransferHeadEntity } from "../entity/TransferHeadEntity";

export class TransferDetailDto {
    public transferHeadTe: TransferHeadEntity;
    public transferHeadTs: TransferHeadEntity;
    public transferDetTeList: TransferDetEntity[];
    public transferDetTsList: TransferDetEntity[];
    public transferDocumentList: TransferDocumentEntity[];

    constructor() {
        this.transferHeadTe = new TransferHeadEntity();
        this.transferHeadTs = new TransferHeadEntity();
        this.transferDetTeList = [];
        this.transferDetTsList = [];
        this.transferDocumentList = [];
    }
}
