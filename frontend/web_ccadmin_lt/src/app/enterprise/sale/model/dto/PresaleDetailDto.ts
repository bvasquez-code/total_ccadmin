import { PresaleDetEntity } from "../entity/PresaleDetEntity";
import { PresaleHeadEntity } from "../entity/PresaleHeadEntity";

export class PresaleDetailDto
{
    public Headboard : PresaleHeadEntity = new PresaleHeadEntity();
    public DetailList : PresaleDetEntity[] = [];

    public constructor()
    {
        this.Headboard = new PresaleHeadEntity();
        this.DetailList = [];
    }
}