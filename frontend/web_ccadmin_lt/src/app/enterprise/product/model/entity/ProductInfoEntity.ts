import { AuditTableEntity } from "src/app/enterprise/shared/model/entity/AuditTableEntity";

export class ProductInfoEntity extends AuditTableEntity
{
    public ProductCod: string = "";
    public Variant: string = "";
    public StoreCod: string = "";
    public NumDigitalStock: number = 0;
    public NumPhysicalStock: number = 0;
}