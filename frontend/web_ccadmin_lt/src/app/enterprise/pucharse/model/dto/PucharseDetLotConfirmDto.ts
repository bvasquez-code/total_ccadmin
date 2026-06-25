import { PucharseDetEntity } from "../entity/PucharseDetEntity";

export class PucharseDetLotConfirmDto {

    public pucharseDet: PucharseDetEntity;
    public lotDetailList: PucharseDetEntity[];
    public WarehouseCod: string;

    constructor()
    {
        this.pucharseDet = new PucharseDetEntity();
        this.lotDetailList = [];
        this.WarehouseCod = "";
    }
}
