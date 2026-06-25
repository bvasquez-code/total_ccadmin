import { ProductInfoDto } from "src/app/enterprise/product/model/dto/ProductInfoDto";
import { AuditTableEntity } from '../../../shared/model/entity/AuditTableEntity';

export class PresaleDetEntity extends AuditTableEntity
{
    public PresaleCod : string = "";
    public ItemNumber : number = 0;
    public ProductCod : string = "";
    public Variant : string = "";
    public NumUnit : number = 0;
    public NumUnitPrice : number = 0;
    public NumDiscount : number = 0;
    public NumUnitPriceSale : number = 0;
    public NumTotalPrice : number = 0;
    public LotNumber : string = "";
    public ExpirationDate : Date | any = null;

    public ProductInfo : ProductInfoDto = new ProductInfoDto();

    public constructor()
    {
        super();
    }

    Build(ProductInfo : ProductInfoDto,Variant : string):void
    {
        this.ProductCod = ProductInfo.Product.ProductCod;
        this.Variant = Variant;
        this.NumUnit = 0;
        this.NumUnitPrice = ProductInfo.Config.NumPrice;
        this.NumDiscount = 0;
        this.NumUnitPriceSale = this.NumUnitPrice - this.NumDiscount;
        this.NumTotalPrice = this.NumUnitPriceSale * this.NumUnit;
        this.ProductInfo = ProductInfo;

    }

    Update(NumUnit : number):void
    {
        this.NumUnit = NumUnit;
        this.NumUnitPriceSale = this.NumUnitPrice - this.NumDiscount;
        this.NumTotalPrice = this.NumUnitPriceSale * this.NumUnit;
    }

    getNameSummary() : string
    {
        let NameSummary : string = "";
        NameSummary = this.ProductInfo.Product.ProductName;
        NameSummary = NameSummary + " (" + this.ProductInfo.VariantList.find( e => e.Variant === this.Variant )?.VariantDesc + ")";
        return NameSummary;
    }

    SetDataSession( DataSession : any )
    {
        this.PresaleCod = DataSession.PresaleCod;
        this.ItemNumber = DataSession.ItemNumber ?? 0;
        this.ProductCod = DataSession.ProductCod;
        this.Variant = DataSession.Variant;
        this.NumUnit = DataSession.NumUnit;
        this.NumUnitPrice = DataSession.NumUnitPrice;
        this.NumDiscount = DataSession.NumDiscount;
        this.NumUnitPriceSale = DataSession.NumUnitPriceSale;
        this.NumTotalPrice = DataSession.NumTotalPrice;
        this.LotNumber = DataSession.LotNumber ?? "";
        this.ExpirationDate = DataSession.ExpirationDate ?? null;
        this.ProductInfo.SetDataSession(DataSession.ProductInfo);
        this.addSession(DataSession);
    }

}
