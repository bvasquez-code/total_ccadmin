import { AuditTableEntity } from "src/app/enterprise/shared/model/entity/AuditTableEntity";

export class CreditNoteDetEntity extends AuditTableEntity {
    public CreditNoteCod: string = '';
    public ItemNumber: number = 0;
    public ProductCod: string = '';
    public Variant: string = '';
    public NumUnit: number = 0;
    public NumUnitPriceSale: number = 0;
    public NumTotalPrice: number = 0;
    public NumUnitStockReturned : number = 0;
    public ProductUnitName: string = 'NIU';
    public ProductUnitFactor: number = 1;
    public LotNumber: string = '';
    public ExpirationDate: Date | any = null;

    constructor() {
        super();
    }
}
