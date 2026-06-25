import { SidebarSubMenuConfigDto } from "./SidebarSubMenuConfigDto";

export class SidebarMenuConfigDto {
    public permission: string = "";
    public label: string = "";
    public icon: string = "";
    public children: SidebarSubMenuConfigDto[] = [];

    constructor(init?: Partial<SidebarMenuConfigDto>) {
        Object.assign(this, init);
        this.children = (init?.children || []).map(child => new SidebarSubMenuConfigDto(child));
    }
}
