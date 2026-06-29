import { AuditTableEntity } from "src/app/enterprise/shared/model/entity/AuditTableEntity";

export class PucharseDetDeliveryEntity extends AuditTableEntity
{
    public PucharseCod: string;
    public ItemNumber: number;
    public ProductCod: string;
    public Variant: string;
    public WarehouseCod: string;
    public NumUnit: number;
    public ProductUnitName: string;
    public ProductUnitFactor: number;
    public LotNumber: string;
    public ExpirationDate: Date | any;

    constructor() {
        super();
        this.PucharseCod = '';
        this.ItemNumber = 0;
        this.ProductCod = '';
        this.Variant = '';
        this.WarehouseCod = '';
        this.NumUnit = 0;
        this.ProductUnitName = 'NIU';
        this.ProductUnitFactor = 1;
        this.LotNumber = '';
        this.ExpirationDate = null;
    }
}
