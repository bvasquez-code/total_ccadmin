export class SidebarSubMenuConfigDto {
    public permission: string = "";
    public label: string = "";
    public url: string = "";
    public urlPosition?: string;
    public urlShade?: string;
    public icon?: string;
    public isVisible?: boolean;

    constructor(init?: Partial<SidebarSubMenuConfigDto>) {
        Object.assign(this, init);
    }
}
