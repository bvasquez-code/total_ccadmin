import { AuditTableEntity } from "src/app/enterprise/shared/model/entity/AuditTableEntity";

export class CounterfoilEntity extends AuditTableEntity {

    public CounterfoilCod: string;
    public DocumentType: string;
    public Series: string;
    public Correlative: number;
    public IsAutomatic: string;   // S/N
    public GroupDocument: string;

    constructor() {
        super();
        this.CounterfoilCod = '';
        this.DocumentType = '';
        this.Series = '';
        this.Correlative = 0;
        this.IsAutomatic = 'S';
        this.GroupDocument = '';
    }
}
