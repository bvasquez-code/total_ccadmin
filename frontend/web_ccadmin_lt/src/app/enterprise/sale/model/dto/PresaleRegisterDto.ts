import { PresaleDetEntity } from "../entity/PresaleDetEntity";
import { PresaleChannelEntity } from "../entity/PresaleChannelEntity";
import { PresaleHeadEntity } from "../entity/PresaleHeadEntity";
import { SaleBillingEntity } from "../entity/SaleBillingEntity";

export class PresaleRegisterDto
{
    public Headboard : PresaleHeadEntity = new PresaleHeadEntity();
    public DetailList : PresaleDetEntity[] = [];
    public PresaleChannel : PresaleChannelEntity = new PresaleChannelEntity();
    public SaleBilling: SaleBillingEntity = new SaleBillingEntity();
    public CreditNoteCod: string = "";

    public constructor()
    {
        this.Headboard = new PresaleHeadEntity();
        this.DetailList = [];
        this.PresaleChannel = new PresaleChannelEntity();
        this.SaleBilling = new SaleBillingEntity();
        this.CreditNoteCod = "";
    }

    public ReBuild():void
    {
        this.Headboard.NumPriceSubTotal = this.toMoney(this.GetNumPriceSubTotal());
        this.Headboard.NumDiscount = this.toMoney(this.GetNumDiscount());
        this.Headboard.NumTotalPrice = this.toMoney(
            this.Headboard.NumPriceSubTotal - this.Headboard.NumDiscount
        );
    }

    GetNumPriceSubTotal()
    {
        let NumPriceSubTotal : number = 0;
        for(let item of this.DetailList)
        {
            NumPriceSubTotal = this.toMoney(
                NumPriceSubTotal + this.toMoney(item.NumUnit * item.NumUnitPrice)
            );
        }
        return NumPriceSubTotal;
    }

    GetNumDiscount()
    {
        let NumDiscount : number = 0;
        for(let item of this.DetailList)
        {
            NumDiscount = this.toMoney(
                NumDiscount + this.toMoney(item.NumUnit * item.NumDiscount)
            );
        }
        return NumDiscount;
    }

    SetDataSession( DataSession : any )
    {
        this.Headboard.SetDataSession( DataSession.Headboard );
        this.PresaleChannel = Object.assign(
            new PresaleChannelEntity(),
            DataSession.PresaleChannel ?? {}
        );
        this.SaleBilling = Object.assign(
            new SaleBillingEntity(),
            DataSession.SaleBilling ?? {}
        );
        this.CreditNoteCod = DataSession.CreditNoteCod ?? "";

        for(let Item of DataSession.DetailList)
        {
            let PresaleDet : PresaleDetEntity = new PresaleDetEntity();
            PresaleDet.SetDataSession(Item);
            this.DetailList.push(PresaleDet);
        }
        this.ReBuild();
    }

    private toMoney(value: number): number
    {
        return Math.round((Number(value || 0) + Number.EPSILON) * 100) / 100;
    }
}
