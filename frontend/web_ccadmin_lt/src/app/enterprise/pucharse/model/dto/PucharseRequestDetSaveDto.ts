import { PucharseRequestDetEntity } from "../entity/PucharseRequestDetEntity";
import { PucharseRequestHeadEntity } from "../entity/PucharseRequestHeadEntity";

export class PucharseRequestDetSaveDto {
    public Headboard: PucharseRequestHeadEntity;
    public Detail: PucharseRequestDetEntity;

    constructor() {
        this.Headboard = new PucharseRequestHeadEntity();
        this.Detail = new PucharseRequestDetEntity();
    }
}
