import { ClientEntity } from "src/app/enterprise/client/model/entity/ClientEntity";
import { AuditTableEntity } from "src/app/enterprise/shared/model/entity/AuditTableEntity";

export class PresaleHeadEntity extends AuditTableEntity
{
    public PresaleCod: string = "";
    public StoreCod: string = "";
    public ClientCod?: string;
    public NumPriceSubTotal: number = 0;
    public NumDiscount: number = 0;
    public NumTotalPrice: number = 0;
    public NumTotalPriceNoTax: number = 0;
    public NumTotalTax: number = 0;
    public Commenter: string = "";
    public PeriodId: number = 0;
    public SaleStatus: string = "";
    public CurrencyCod: string = "";
    public CurrencyCodSys: string = "";
    public NumExchangevalue: number = 0;
    public IsPaid: string = "";

    public Client : ClientEntity  = new ClientEntity();

    public constructor()
    {
        super();
    }

    SetDataSession( DataSession : any )
    {
        this.PresaleCod = DataSession.PresaleCod;
        this.StoreCod = DataSession.StoreCod;
        this.ClientCod = DataSession.ClientCod;
        this.NumPriceSubTotal = DataSession.NumPriceSubTotal;
        this.NumDiscount = DataSession.NumDiscount;
        this.NumTotalPrice = DataSession.NumTotalPrice;
        this.NumTotalPriceNoTax = DataSession.NumTotalPriceNoTax;
        this.NumTotalTax = DataSession.NumTotalTax;
        this.Commenter = DataSession.Commenter;
        this.PeriodId = DataSession.PeriodId;
        this.SaleStatus = DataSession.SaleStatus;
        this.CurrencyCod = DataSession.CurrencyCod;
        this.CurrencyCodSys = DataSession.CurrencyCodSys;
        this.NumExchangevalue = DataSession.NumExchangevalue;
        this.IsPaid = DataSession.IsPaid;
        this.Client = DataSession.Client;
        this.addSession(DataSession);
    }
}