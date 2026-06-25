import { AuditTableEntity } from '../../../shared/model/entity/AuditTableEntity';
import { ProfileMenuEntity } from './ProfileMenuEntity';
export class AppProfileEntity extends AuditTableEntity
{
    public ProfileCod: string = "";
	public Name: string = "";
	public Description: string = "";

    public permissionsList : ProfileMenuEntity[] = [];

    public constructor()
    {
        super();
    }
}