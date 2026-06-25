export class ProductSearchDto
{
    public Query : string = "";
    public Page : number = 1;
    public BrandCod : string = "";
    public CategoryCod : string = "";
    public StoreCod : string = "";
    public StockMin : number = 0;
    public SortedBy : string = "trend";
    public DirectionSortedBy : string = "desc";
}