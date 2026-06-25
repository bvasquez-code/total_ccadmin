import { PucharseDetDeliveryEntity } from "../entity/PucharseDetDeliveryEntity";
import { PucharseDetEntity } from "../entity/PucharseDetEntity";
import { PucharseHeadEntity } from "../entity/PucharseHeadEntity";

export class PucharseRegisterDto
{
    public PucharseReqCod: string;
    public PucharseCod: string;
    public Headboard : PucharseHeadEntity;
    public DetailList : PucharseDetEntity[];
    public DeliveryList: PucharseDetDeliveryEntity[];
    
    constructor() {
        this.PucharseReqCod = '';
        this.PucharseCod = '';
        this.Headboard = new PucharseHeadEntity();
        this.DetailList = [];
        this.DeliveryList = [];
    }
}