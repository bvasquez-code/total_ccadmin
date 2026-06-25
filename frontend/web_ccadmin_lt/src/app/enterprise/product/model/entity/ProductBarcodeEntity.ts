import { AuditTableEntity } from "src/app/enterprise/shared/model/entity/AuditTableEntity";

export class ProductBarcodeEntity extends AuditTableEntity
{
    public BarCode : string = "";
    public ProductCod : string = "";
}