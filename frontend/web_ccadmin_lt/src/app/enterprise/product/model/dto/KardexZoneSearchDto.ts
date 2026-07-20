import { SearchDto } from "src/app/enterprise/shared/model/dto/SearchDto";

export class KardexZoneSearchDto extends SearchDto {
    public ZoneStockMoved: string = "";
    public TypeOperation: string = "";
}
