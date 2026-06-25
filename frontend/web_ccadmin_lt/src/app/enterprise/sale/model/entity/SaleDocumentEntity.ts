import { AuditTableEntity } from "src/app/enterprise/shared/model/entity/AuditTableEntity";

export class SaleDocumentEntity extends AuditTableEntity{

    public DocumentCod : string;
    public CounterfoilCod : string;
    public SaleCod : string;

    constructor(){
        super();
        this.DocumentCod = "";
        this.CounterfoilCod = "";
        this.SaleCod = "";
    }
}
