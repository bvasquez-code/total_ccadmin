import { AuditTableEntity } from "src/app/enterprise/shared/model/entity/AuditTableEntity";

export class CounterfoilStoreEntity extends AuditTableEntity {

    public CounterfoilCod: string;
    public StoreCod: string;

    constructor() {
        super();
        this.CounterfoilCod = '';
        this.StoreCod = '';
    }
}
