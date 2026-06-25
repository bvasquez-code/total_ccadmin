import { AppMenuEntity } from "src/app/enterprise/menu/model/entity/AppMenuEntity";

export class AppMenuStructureDto
{
    public MenuDad : AppMenuEntity;

    public MenuChildList : AppMenuEntity[];

    public constructor()
    {
        this.MenuDad = new AppMenuEntity();
        this.MenuChildList = [];
    }
}