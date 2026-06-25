import { ResponseWsDto } from "../model/dto/ResponseWsDto";
import { SearchDto } from "../model/dto/SearchDto";

export interface ICrudService<T, ID>
{
    FindById(Id : ID): Promise<ResponseWsDto>;
    FindAll(Search : SearchDto): Promise<ResponseWsDto>;
    Save(Entity : T): Promise<ResponseWsDto>;
    SaveAll(EntityList : T[]): Promise<ResponseWsDto>;
    FindAllById(IdList : ID[]): Promise<ResponseWsDto>;
}