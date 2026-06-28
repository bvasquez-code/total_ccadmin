import { AuditTableEntity } from "src/app/enterprise/shared/model/entity/AuditTableEntity";

export class ProductInfoWarehouseEntity extends AuditTableEntity
{
    public ProductCod: string = "";
    public Variant: string = "";
    public WarehouseCod: string = "";
    public NumDigitalStock: number = 0;
    public NumPhysicalStock: number = 0;
    public NumUnavailableStock: number = 0;
    public NumReservedStock: number = 0;
    public NumTotalStock: number = 0;
}
