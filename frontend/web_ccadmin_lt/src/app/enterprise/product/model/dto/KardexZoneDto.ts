import { KardexZoneEntity } from "../entity/KardexZoneEntity";
import { ProductEntity } from "../entity/ProductEntity";

export class KardexZoneDto {
    public kardexZone: KardexZoneEntity = new KardexZoneEntity();
    public product: ProductEntity = new ProductEntity();
}
