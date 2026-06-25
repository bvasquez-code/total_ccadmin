import { AuditTableEntity } from "src/app/enterprise/shared/model/entity/AuditTableEntity";

export class ProductConfigEntity extends AuditTableEntity
{
    public ProductCod: string = "";
    public NumPrice: number = 0;
    public NumMaxStock: number = 0;
    public NumMinStock: number = 0;
    public IsDiscontable: string = "";
    public DiscountType: string = "";
    public NumDiscountMax: number = 0;
    public Version: string = "";
}