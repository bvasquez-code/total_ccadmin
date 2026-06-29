import { AuditTableEntity } from '../../../shared/model/entity/AuditTableEntity';
import { ProductEntity } from '../../../product/model/entity/ProductEntity';
export class SaleDetEntity extends AuditTableEntity
{
    public SaleCod: string = "";
    public ItemNumber: number = 0;
	public ProductCod: string = "";
	public Variant: string = "";
	public NumUnit: number = 0;
	public NumUnitPrice: number = 0;
	public NumDiscount: number = 0;
	public NumUnitPriceSale: number = 0;
	public NumTotalPrice: number = 0;
	public IsAppliedTax: string = "";
	public ProductUnitName: string = "NIU";
	public ProductUnitFactor: number = 1;
	public LotNumber: string = "";
	public ExpirationDate: Date | any = null;

	public Product : ProductEntity = new ProductEntity();

    public constructor()
    {
        super();
    }
}
