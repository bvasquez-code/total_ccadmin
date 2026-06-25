import { SessionStorageDto } from '../../../compartido/entity/SessionStorageDto';
export class AuditTableEntity
{
    public CreationUser: string = "";
    public CreationDate: Date = new Date();
    public ModifyUser?: any = "";
    public ModifyDate: Date = new Date();
    public Status: string = "A";

    public constructor()
    {
        
    }

    public addSession(DataSession : any)
    {
        this.CreationUser = DataSession.CreationUser;
        this.CreationDate = DataSession.CreationDate;
        this.ModifyUser = DataSession.ModifyUser;
        this.ModifyDate = DataSession.ModifyDate;
        this.Status = DataSession.Status;
    }

    
}