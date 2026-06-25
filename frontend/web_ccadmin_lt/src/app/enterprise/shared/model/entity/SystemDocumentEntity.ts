import { AuditTableEntity } from "./AuditTableEntity";

export class SystemDocumentEntity extends AuditTableEntity {

    public DocumentCod: string = "";
    public DocumentType: string = "";
    public ReferenceCod: string = "";
    public Content: string = "";

    constructor() {
        super();
    }
}
