import { AuditTableEntity } from "src/app/enterprise/shared/model/entity/AuditTableEntity";
import { AppFileEntity } from "src/app/enterprise/system/model/entity/AppFileEntity";

export class ProductPictureEntity extends AuditTableEntity
{
    public ProductCod : string = "";
    public FileCod : string = "";
    public IsPrincipal : string = "";

    public appFile : AppFileEntity = new AppFileEntity();
}