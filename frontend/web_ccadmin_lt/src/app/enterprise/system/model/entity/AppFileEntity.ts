import { AuditTableEntity } from "src/app/enterprise/shared/model/entity/AuditTableEntity";

export class AppFileEntity extends AuditTableEntity {

    public FileCod: string;
    public Name: string;
    public Description: string;
    public Route: string;
    public FileType: string;

    constructor() {
        super();
        this.FileCod = '';
        this.Name = '';
        this.Description = '';
        this.Route = '';
        this.FileType = '';
    }
}
