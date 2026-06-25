import { AuditTableEntity } from "src/app/enterprise/shared/model/entity/AuditTableEntity";

export class CreditNoteHeadEntity extends AuditTableEntity {

    public CreditNoteCod: string = '';
    public SaleCod: string = '';
    public StoreCod: string = '';
    public ClientCod: string = '';
    public NumTotalPrice: number = 0; // Inicializado con 0
    public Commenter: string = '';
    public PeriodId: number = 0; // Inicializado con 0
    public CreditNoteStatus: string = '';
    public CurrencyCod: string = '';
    public CurrencyCodSys: string = '';
    public NumExchangevalue: number = 0; // Inicializado con 0
    public IsPaid: string = '';
    public IsStockReturned: string = 'N';
    public TypeCreditNote: string = '';

    constructor() {
        super(); 
    }
}
