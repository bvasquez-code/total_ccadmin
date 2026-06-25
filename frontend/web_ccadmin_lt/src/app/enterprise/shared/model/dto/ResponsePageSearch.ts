export class ResponsePageSearch <T>
{
    public resultSearch  : T[] = [];
    public TotalPages : number = 0;
    public TotalResult : number = 0;
    public StarResult : number = 0;
    public EndResult : number = 0;
    public Page : number = 0;

    public constructor()
    {
        
    }

}