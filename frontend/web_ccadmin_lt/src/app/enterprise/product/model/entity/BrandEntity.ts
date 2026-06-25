import { AuditTableEntity } from "src/app/enterprise/shared/model/entity/AuditTableEntity";

export class BrandEntity extends AuditTableEntity
{
    public BrandCod : string = "";
    public BrandName : string = "";

    constructor()
    {
        super();
    }
}