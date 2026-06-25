import { AuditTableEntity } from "src/app/enterprise/shared/model/entity/AuditTableEntity";

export class CashRegisterEntity extends AuditTableEntity {

    public RegisterCod: string;
    public StoreCod: string;
    public Name: string;
    public Description: string;
    public SerialNumber: string;

    constructor() {
        super();
        this.RegisterCod = '';
        this.StoreCod = '';
        this.Name = '';
        this.Description = '';
        this.SerialNumber = '';
    }
}
