import { TransferDetEntity } from "../entity/TransferDetEntity";

export class TransferDetRegisterMassiveDto {

    public transferDetList: TransferDetEntity[];

    constructor() {
        this.transferDetList = [];
    }

    public add(transferDet: TransferDetEntity) {
        this.transferDetList.push(transferDet);
    }

    public static build(transferDetList: TransferDetEntity[]): TransferDetRegisterMassiveDto {
        const dto = new TransferDetRegisterMassiveDto();
        dto.transferDetList = transferDetList;
        return dto;
    }

    public static buildSimple(transferDet: TransferDetEntity): TransferDetRegisterMassiveDto {
        const dto = new TransferDetRegisterMassiveDto();
        dto.transferDetList.push(transferDet);
        return dto;
    }
}