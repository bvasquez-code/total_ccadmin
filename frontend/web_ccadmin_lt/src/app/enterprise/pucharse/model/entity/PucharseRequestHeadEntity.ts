import { AuditTableEntity } from "src/app/enterprise/shared/model/entity/AuditTableEntity";

export class PucharseRequestHeadEntity extends AuditTableEntity
{
    public PucharseReqCod: string;
    public StoreCod: string;
    public ExternalCod: string;
    public DealerCod: string;
    public Commenter: string;
    public PurchaseStatus: string;
    public CurrencyCod: string;
    public CurrencyCodSys: string;
    public NumExchangevalue: number;
    public NumTotalPrice: number;

    constructor() {
        super();
        this.PucharseReqCod = '';
        this.StoreCod = '';
        this.ExternalCod = '';
        this.DealerCod = '';
        this.Commenter = '';
        this.PurchaseStatus = '';
        this.CurrencyCod = '';
        this.CurrencyCodSys = '';
        this.NumExchangevalue = 0;
        this.NumTotalPrice = 0;
    }
}