import { AuditTableEntity } from "src/app/enterprise/shared/model/entity/AuditTableEntity";

export class CreditNoteApplicationEntity extends AuditTableEntity {
    public ApplicationId: number = 0;
    public CreditNoteCod: string = "";
    public SaleCod: string = "";
    public TrxPaymentId: number = 0;
    public AmountApplied: number = 0;
}
