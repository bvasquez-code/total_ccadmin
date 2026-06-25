import { AuditTableEntity } from "src/app/enterprise/shared/model/entity/AuditTableEntity";

export class ProductVariantEntity extends AuditTableEntity
{
    public ProductCod: string = "";
    public Variant: string = "";
    public VariantDesc: string = "";
}