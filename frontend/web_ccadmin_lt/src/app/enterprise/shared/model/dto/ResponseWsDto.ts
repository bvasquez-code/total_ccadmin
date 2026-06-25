import { ResponseAdditionalDto } from "./ResponseAdditionalDto";

export class ResponseWsDto{

    public Status : string = "";
    public Message : string = "";
    public Data : any = null;
    public ErrorStatus : boolean = false;
    public ErrorID : number = 0;
    public DataAdditional : ResponseAdditionalDto[] = [];

    public constructor()
    {

    }

    addError(Error: any) {
        this.ErrorStatus = true;
        this.Message = Error.Message;
    }

}