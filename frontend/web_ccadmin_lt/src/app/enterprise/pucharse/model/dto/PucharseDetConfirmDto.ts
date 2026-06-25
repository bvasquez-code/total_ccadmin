import { PucharseDetDeliveryEntity } from "../entity/PucharseDetDeliveryEntity";
import { PucharseDetEntity } from "../entity/PucharseDetEntity";

export class PucharseDetConfirmDto {

    public pucharseDet : PucharseDetEntity;
    public pucharseDetDelivery : PucharseDetDeliveryEntity;
    
    constructor()
    {
        this.pucharseDet = new PucharseDetEntity();
        this.pucharseDetDelivery = new PucharseDetDeliveryEntity();
    }
}
