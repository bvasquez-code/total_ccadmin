import { ProductEntity } from "src/app/enterprise/product/model/entity/ProductEntity";
import { AuditTableEntity } from "src/app/enterprise/shared/model/entity/AuditTableEntity";

export class PucharseRequestDetEntity extends AuditTableEntity
{
    public PucharseReqCod: string;
    public ItemNumber: number;
    public ProductCod: string;
    public Variant: string;
    public NumUnit: number;
    public NumUnitPrice: number;
    public NumTotalPrice: number;
    public ProductUnitName: string;
    public ProductUnitFactor: number;
    public LotNumber: string;
    public ExpirationDate: Date | any;

    public Product : ProductEntity;

    constructor() {
        super();
        this.PucharseReqCod = '';
        this.ItemNumber = 0;
        this.ProductCod = '';
        this.Variant = '';
        this.NumUnit = 0;
        this.NumUnitPrice = 0;
        this.NumTotalPrice = 0;
        this.ProductUnitName = 'NIU';
        this.ProductUnitFactor = 1;
        this.LotNumber = '';
        this.ExpirationDate = null;
        this.Product = new ProductEntity();
    }
}
