import { HeaderTableGenericDto } from "./HeaderTableGenericDto";
import { InfoTablaGenericDto } from "./InfoTablaGenericDto";

export class DataTablaGeneticDto <T>
{
    public NameTable : string = "";
    public Headers : HeaderTableGenericDto[] = [];
    public DataTable : InfoTablaGenericDto <T> = new InfoTablaGenericDto();

    constructor()
    {
    }

    init(Headers : HeaderTableGenericDto[],DataTable : InfoTablaGenericDto <T>,NameTable : string = "")
    {
        this.Headers = Headers;
        this.DataTable = DataTable;
        this.NameTable = NameTable;
    }

}