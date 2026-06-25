import { TransferDetEntity } from "../entity/TransferDetEntity";

export class TransferDispatchDto {
    public transferCod: string;
    public user: string;
    public transportModeCod: string;
    public reasonTransferCod: string;
    public vehiclePlate: string;
    public driverDocType: string;
    public driverDocNumber: string;
    public driverLicenseNumber: string;
    public carrierRuc: string;
    public carrierName: string;
    public observation: string;
    public detailListRequest: TransferDetEntity[];

    constructor() {
        this.transferCod = '';
        this.user = '';
        this.transportModeCod = '';
        this.reasonTransferCod = '';
        this.vehiclePlate = '';
        this.driverDocType = '';
        this.driverDocNumber = '';
        this.driverLicenseNumber = '';
        this.carrierRuc = '';
        this.carrierName = '';
        this.observation = '';
        this.detailListRequest = [];
    }
}
