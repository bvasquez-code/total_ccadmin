import { AuditTableEntity } from '../../../shared/model/entity/AuditTableEntity';
export class UserProfileEntity extends AuditTableEntity
{
    public UserCod: string = "";
	public ProfileCod: string  = "";

    public constructor()
    {
        super();
    }
}