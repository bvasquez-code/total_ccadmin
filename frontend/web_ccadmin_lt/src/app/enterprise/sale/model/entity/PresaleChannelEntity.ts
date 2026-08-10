import { AuditTableEntity } from "src/app/enterprise/shared/model/entity/AuditTableEntity";
import { SaleConstants } from "../constants/SaleConstants";

export class PresaleChannelEntity extends AuditTableEntity
{
    public PresaleCod: string = "";
    public ChannelCod: string = SaleConstants.COMMERCIAL_CHANNEL_IN_PERSON;

    public constructor()
    {
        super();
    }
}
