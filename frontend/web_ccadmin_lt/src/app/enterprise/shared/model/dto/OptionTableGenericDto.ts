export class OptionTableGenericDto{

    public Type: 'Modal' | 'Url' = 'Modal';
    public ID? : string = "";
    public Name? : string = "";
    public Url?  : string = "";
    public Function?: (...args: any[]) => any;
    public FunctionUrl?: (...args: any[]) => any;
        
    constructor()
    {

    }

}