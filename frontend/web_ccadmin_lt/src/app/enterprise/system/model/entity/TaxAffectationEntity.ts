import { AuditTableEntity } from "src/app/enterprise/shared/model/entity/AuditTableEntity";

export class TaxAffectationEntity extends AuditTableEntity {
    public TaxAffectationCod: string = "";
    public TaxCod: string = "";
    public Name: string = "";
    public Description: string = "";
    public IsTaxed: string = "N";
}
