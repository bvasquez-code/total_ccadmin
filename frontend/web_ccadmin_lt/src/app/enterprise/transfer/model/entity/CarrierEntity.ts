import { AuditTableEntity } from "src/app/enterprise/shared/model/entity/AuditTableEntity";

export class CarrierEntity extends AuditTableEntity {
    public CarrierCod: string;
    public CarrierRuc: string;
    public CarrierName: string;
    public VehiclePlate: string;
    public DriverDocType: string;
    public DriverDocNumber: string;
    public DriverLicenseNumber: string;

    constructor() {
        super();
        this.CarrierCod = "";
        this.CarrierRuc = "";
        this.CarrierName = "";
        this.VehiclePlate = "";
        this.DriverDocType = "";
        this.DriverDocNumber = "";
        this.DriverLicenseNumber = "";
    }
}
