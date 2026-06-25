import { AuditTableEntity } from "src/app/enterprise/shared/model/entity/AuditTableEntity";

export class PucharseHeadEntity extends AuditTableEntity {
    public PucharseCod: string;
    public StoreCod: string;
    public PucharseReqCod: string;
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
      this.PucharseCod = '';
      this.StoreCod = '';
      this.PucharseReqCod = '';
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
