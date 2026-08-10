import { StoreVirtualConfigEntity } from "src/app/enterprise/sale/model/entity/StoreVirtualConfigEntity";
import { StoreEntity } from "src/app/enterprise/shared/model/entity/StoreEntity";

export class StoreVirtualConfigRegisterDto
{
    public Store: StoreEntity = new StoreEntity();
    public Config: StoreVirtualConfigEntity = new StoreVirtualConfigEntity();
}
