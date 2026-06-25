import { AuditTableEntity } from "src/app/enterprise/shared/model/entity/AuditTableEntity";

export class ProductEntity extends AuditTableEntity
{
    
    public ProductCod: string = "";
    public CategoryCod: string = "";
    public BrandCod: string = "";
    public ProductName: string = "";
    public ProductDesc: string = "";
}