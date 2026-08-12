import { PersonEntity } from "src/app/enterprise/person/model/entity/PersonEntity";
import { AuditTableEntity } from "src/app/enterprise/shared/model/entity/AuditTableEntity";

export class SaleBillingEntity extends AuditTableEntity {
    public SaleCod: string = "";
    public PersonCod: string = "";
    public DocumentTypeRequest: string = "";
    public DocumentType: string = "";
    public DocumentNum: string = "";
    public LegalName: string = "";
    public CommercialName: string = "";
    public Address: string = "";
    public UbigeoCod: string = "";
    public Person: PersonEntity = new PersonEntity();
}
