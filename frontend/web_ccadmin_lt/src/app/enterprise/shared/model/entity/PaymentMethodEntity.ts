import { AuditTableEntity } from "src/app/enterprise/shared/model/entity/AuditTableEntity";

export class PaymentMethodEntity extends AuditTableEntity {

    public PaymentMethodCod: string = "";
    public Name: string = "";
    public Description: string = "";
    public PaymentMethodType: string = "";
    public PaymentProcessingMode: string = "D";
    public IsInternalSaleEnabled: string = "S";
    public IsWebSaleEnabled: string = "S";
    public IsPaymentProofRequired: string = "N";
    public FileCod: string = "";
    public Route: string = "";

    constructor() {
        super();
    }
}
