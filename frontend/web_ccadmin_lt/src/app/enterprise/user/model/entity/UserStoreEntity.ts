import { AuditTableEntity } from "../../../shared/model/entity/AuditTableEntity";

export class UserStoreEntity extends AuditTableEntity {
    public UserCod: string = "";
    public StoreCod: string = "";
    public IsMainStore: boolean = false;

    public constructor() {
        super();
    }
}