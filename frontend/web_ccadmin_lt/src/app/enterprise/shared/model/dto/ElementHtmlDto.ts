export class ElementHtmlDto
{
    public IdElement : string = "";
    public ValueElement : string = "";
    public IsVisible : boolean = true;
    public IsEnable : boolean = true;
    public ReadOnly : boolean = false;

    constructor(){

    }
    build(IdElement : string, ValueElement : string, IsVisible : boolean, IsEnable : boolean) : ElementHtmlDto{
        this.IdElement = IdElement;
        this.ValueElement = ValueElement;
        this.IsVisible = IsVisible;
        this.IsEnable = IsEnable;
        return this;
    }   
}