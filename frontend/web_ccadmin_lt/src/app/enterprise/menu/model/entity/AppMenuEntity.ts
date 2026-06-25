import { AuditTableEntity } from "src/app/enterprise/shared/model/entity/AuditTableEntity";

export class AppMenuEntity extends AuditTableEntity
{
    public MenuCod : string = "";
    public Name : string = "";
    public Description : string = "";
    public IsMenuDad : string = "";
    public MenuDadCod : string = "";

    constructor()
    {
        super();
    }
}