export class InfoPageDto
{
    public NameButton : string = "";
    public ValueButton : number = 0;
    public IsActive : boolean = false;
    public IsCurrent : boolean = false;

    constructor(){

    }

    buildPrev(Page : number) : InfoPageDto{
        this.IsActive = true;
        this.NameButton = "Prev";
        this.ValueButton = Page - 1;
        return this;
    }

    buildNext(Page : number) : InfoPageDto{
        this.IsActive = true;
        this.NameButton = "Next";
        this.ValueButton = Page + 1;
        return this;
    }

}