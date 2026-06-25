import { PucharseRequestDetEntity } from "../entity/PucharseRequestDetEntity";
import { PucharseRequestHeadEntity } from "../entity/PucharseRequestHeadEntity";

export class PucharseRequestDetailsDto
{
    public Headboard: PucharseRequestHeadEntity;
    public DetailList: PucharseRequestDetEntity[];

    constructor() {
        this.Headboard = new PucharseRequestHeadEntity();
        this.DetailList = [];
    }
}