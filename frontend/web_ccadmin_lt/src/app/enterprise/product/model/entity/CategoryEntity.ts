import { AuditTableEntity } from "src/app/enterprise/shared/model/entity/AuditTableEntity";

export class CategoryEntity extends AuditTableEntity
{
    public CategoryCod : string = "";
    public CategoryName : string = "";
    public CategoryDadCod : string = "";
    public IsDigital : string = "";
    public IsCategoryDad : string = "";

    constructor()
    {
        super();
    }

}