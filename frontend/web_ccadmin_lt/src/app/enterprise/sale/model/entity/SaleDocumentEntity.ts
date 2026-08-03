import { AuditTableEntity } from "src/app/enterprise/shared/model/entity/AuditTableEntity";
import { ClientEntity } from "src/app/enterprise/client/model/entity/ClientEntity";

export class SaleDocumentEntity extends AuditTableEntity{

    public DocumentCod : string;
    public CounterfoilCod : string;
    public SaleCod : string;
    public DocumentType : string;
    public DocumentRole : string;
    public ClientCod : string;
    public IssueDate : Date | null;
    public Client : ClientEntity;

    constructor(){
        super();
        this.DocumentCod = "";
        this.CounterfoilCod = "";
        this.SaleCod = "";
        this.DocumentType = "";
        this.DocumentRole = "";
        this.ClientCod = "";
        this.IssueDate = null;
        this.Client = new ClientEntity();
    }
}
