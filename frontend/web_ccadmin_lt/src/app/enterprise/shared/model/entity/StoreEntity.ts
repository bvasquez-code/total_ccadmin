import { AuditTableEntity } from "./AuditTableEntity";

export class StoreEntity extends AuditTableEntity {
    
    public StoreCod: string;
    public Name: string;
    public Description: string;
    public Address: string;
    public UbigeoCod: string;

    constructor() {
        super();
        this.StoreCod = '';
        this.Name = '';
        this.Description = '';
        this.Address = '';
        this.UbigeoCod = '';
    }
}
