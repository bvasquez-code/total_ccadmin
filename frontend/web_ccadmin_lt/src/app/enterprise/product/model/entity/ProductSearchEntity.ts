export class ProductSearchEntity{

    public ProductCod : string = "";
    public StoreCod : string = "";
    public ProductName : string = "";
    public ProductDesc : string = "";
    public NumDigitalStock  : number = 0;
    public NumPhysicalStock  : number = 0;
    public NumPrice  : number = 0;
    public NumMaxStock  : number = 0;
    public NumMinStock  : number = 0;
    public IsDiscontable : string = "";
    public DiscountType : string = "";
    public NumDiscountMax : number = 0;
    public BrandCod : string = "";
    public BrandName : string = "";
    public CategoryCod : string = "";
    public CategoryName : string = "";
    public CategoryDadCod : string = "";
    public CategoryDadName : string = "";
    public CurrencyCod : string = "";
    public CurrencySymbol : string = "";
    public FileCod : string = "";
    public FileRoute : string = "";
    public NumTrend : number = 0;

    public constructor()
    {
        
    }
}