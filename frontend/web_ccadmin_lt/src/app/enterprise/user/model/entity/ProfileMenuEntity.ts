import { AuditTableEntity } from '../../../shared/model/entity/AuditTableEntity';
export class ProfileMenuEntity extends AuditTableEntity
{
    public ProfileCod : string = "";
    public MenuCod : string = "";

    public constructor()
    {
        super();
    }
}