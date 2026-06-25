import { AuditTableEntity } from "src/app/enterprise/shared/model/entity/AuditTableEntity";

export class KardexEntity extends AuditTableEntity{

    public kardexID: number;
    public OperationCod: string;
    public ItemNumber: number;
    public SourceTable: string;
    public TypeOperation: string;
    public ProductCod: string;
    public Variant: string;
    public StoreCod: string;
    public WarehouseCod: string;
    public NumStockBefore: number;
    public NumStockMoved: number;
    public NumStockAfter: number;
    public LotNumber: string;
    public ExpirationDate: Date | any;
    public TypeOperationCod : number;

    constructor() {
        super();
        this.kardexID = 0;
        this.OperationCod = '';
        this.ItemNumber = 0;
        this.SourceTable = '';
        this.TypeOperation = '';
        this.ProductCod = '';
        this.Variant = '';
        this.StoreCod = '';
        this.WarehouseCod = '';
        this.NumStockBefore = 0;
        this.NumStockMoved = 0;
        this.NumStockAfter = 0;
        this.LotNumber = '';
        this.ExpirationDate = null;
        this.TypeOperationCod = 0;
    }
}
