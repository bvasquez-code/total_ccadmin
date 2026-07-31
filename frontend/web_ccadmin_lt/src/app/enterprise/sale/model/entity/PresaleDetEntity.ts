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
    public ProductUnitName : string = "NIU";
    public ProductUnitFactor : number = 1;
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
        this.NumUnitPrice = this.toMoney(ProductInfo.Config.NumPrice);
        this.NumDiscount = 0;
        this.recalculateAmounts();
        this.ProductUnitName = ProductInfo.Config.ProductUnitName || "NIU";
        this.ProductUnitFactor = ProductInfo.Config.ProductUnitFactor > 0 ? ProductInfo.Config.ProductUnitFactor : 1;
        this.ProductInfo = ProductInfo;

    }

    Update(NumUnit : number):void
    {
        this.NumUnit = NumUnit;
        this.recalculateAmounts();
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
        this.NumUnitPrice = this.toMoney(DataSession.NumUnitPrice);
        this.NumDiscount = this.toMoney(DataSession.NumDiscount);
        this.recalculateAmounts();
        this.ProductUnitName = DataSession.ProductUnitName ?? "NIU";
        this.ProductUnitFactor = DataSession.ProductUnitFactor > 0 ? DataSession.ProductUnitFactor : 1;
        this.LotNumber = DataSession.LotNumber ?? "";
        this.ExpirationDate = DataSession.ExpirationDate ?? null;
        this.ProductInfo.SetDataSession(DataSession.ProductInfo);
        this.addSession(DataSession);
    }

    private recalculateAmounts(): void
    {
        this.NumUnitPrice = this.toMoney(this.NumUnitPrice);
        this.NumDiscount = this.toMoney(this.NumDiscount);
        this.NumUnitPriceSale = this.toMoney(this.NumUnitPrice - this.NumDiscount);
        this.NumTotalPrice = this.toMoney(this.NumUnitPriceSale * this.NumUnit);
    }

    private toMoney(value: number): number
    {
        return Math.round((Number(value || 0) + Number.EPSILON) * 100) / 100;
    }

}
