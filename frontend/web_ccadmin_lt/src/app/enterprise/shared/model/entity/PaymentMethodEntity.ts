import { AuditTableEntity } from "src/app/enterprise/shared/model/entity/AuditTableEntity";

export class PaymentMethodEntity extends AuditTableEntity {

    public PaymentMethodCod: string = "";
    public Name: string = "";
    public Description: string = "";
    public PaymentMethodType: string = "";
    public FileCod: string = "";
    public Route: string = "";

    constructor() {
        super();
    }
}
