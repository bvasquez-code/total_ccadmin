import { ProductEntity } from "src/app/enterprise/product/model/entity/ProductEntity";
import { AuditTableEntity } from "src/app/enterprise/shared/model/entity/AuditTableEntity";

export class PucharseDetEntity extends AuditTableEntity {
    public PucharseCod: string;
    public ItemNumber: number;
    public ProductCod: string;
    public Variant: string;
    public NumUnit: number;
    public NumUnitPrice: number;
    public NumTotalPrice: number;
    public IsKardexAffected : string;
    public NumUnitDelivered : number;
    public LotNumber: string;
    public ExpirationDate: Date | any;
    public Product: ProductEntity; // Assuming ProductEntity is a separate TypeScript class
  
    constructor() {
      super();
      this.PucharseCod = '';
      this.ItemNumber = 0;
      this.ProductCod = '';
      this.Variant = '';
      this.NumUnit = 0;
      this.NumUnitPrice = 0;
      this.NumTotalPrice = 0;
      this.IsKardexAffected = "N";
      this.NumUnitDelivered = 0;
      this.LotNumber = '';
      this.ExpirationDate = null;
      this.Product = new ProductEntity();
    }
}
