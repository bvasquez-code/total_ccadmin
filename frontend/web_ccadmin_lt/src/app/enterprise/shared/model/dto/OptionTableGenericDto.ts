export class OptionTableGenericDto{

    public Type: 'Modal' | 'Url' | 'Action' = 'Modal';
    public Action?: (...args: any[]) => void;
    public ID? : string = "";
    public Name? : string = "";
    public Url?  : string = "";
    public Title?: string = "";
    public Function?: (...args: any[]) => any;
    public FunctionUrl?: (...args: any[]) => any;
        
    constructor()
    {

    }

}
