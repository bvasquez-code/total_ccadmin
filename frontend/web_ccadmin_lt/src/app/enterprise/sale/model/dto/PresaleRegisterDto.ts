import { PresaleDetEntity } from "../entity/PresaleDetEntity";
import { PresaleHeadEntity } from "../entity/PresaleHeadEntity";

export class PresaleRegisterDto
{
    public Headboard : PresaleHeadEntity = new PresaleHeadEntity();
    public DetailList : PresaleDetEntity[] = [];

    public constructor()
    {
        this.Headboard = new PresaleHeadEntity();
        this.DetailList = [];
    }

    public ReBuild():void
    {
        this.Headboard.NumPriceSubTotal = this.GetNumPriceSubTotal();
        this.Headboard.NumDiscount = this.GetNumDiscount();
        this.Headboard.NumTotalPrice = this.Headboard.NumPriceSubTotal - this.Headboard.NumDiscount;     
    }

    GetNumPriceSubTotal()
    {
        let NumPriceSubTotal : number = 0;
        for(let item of this.DetailList)
        {
            NumPriceSubTotal = NumPriceSubTotal + item.NumUnit * item.NumUnitPrice;
        }
        return NumPriceSubTotal;
    }

    GetNumDiscount()
    {
        let NumDiscount : number = 0;
        for(let item of this.DetailList)
        {
            NumDiscount = NumDiscount + item.NumUnit * item.NumDiscount
        }
        return NumDiscount;
    }

    SetDataSession( DataSession : any )
    {
        this.Headboard.SetDataSession( DataSession.Headboard );

        for(let Item of DataSession.DetailList)
        {
            let PresaleDet : PresaleDetEntity = new PresaleDetEntity();
            PresaleDet.SetDataSession(Item);
            this.DetailList.push(PresaleDet);
        }
    }
}