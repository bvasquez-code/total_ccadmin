import { AuditTableEntity } from "src/app/enterprise/shared/model/entity/AuditTableEntity";

export class CreditNoteDocumentEntity extends AuditTableEntity {

    public DocumentCod : string = "";
    public CounterfoilCod : string = "";
    public CreditNoteCod : string = "";

    constructor(){
        super();
    }
}
