import { TransferDetEntity } from "../entity/TransferDetEntity";

export class TransferReceiveDto {
    public transferCod: string;
    public user: string;
    public observation: string;
    public typeOperation: string;
    public detailListReceive: TransferDetEntity[];

    constructor() {
        this.transferCod = '';
        this.user = '';
        this.observation = '';
        this.typeOperation = '';
        this.detailListReceive = [];
    }
}
