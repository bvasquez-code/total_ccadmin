import { ResponsePageSearch } from "./ResponsePageSearch";

export class InfoTablaGenericDto <T>
{
    public keys? : string[] = [];
    public data : ResponsePageSearch<T> = new ResponsePageSearch();
}