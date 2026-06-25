import { ResponsePageSearch } from "../../shared/model/dto/ResponsePageSearch";

export class RespuestaPaginacionDto<T>{

    public listaResultado : any[] = [];
    public num_resultados : number = 0;
    public num_total_paginas : number = 0;
    public num_pagina_actual : number = 0;
    public num_resultados_pagina : number = 0;

    public item_inicio : number = 0; 
    public item_fin : number = 0;

    constructor()
    {

    }

    addResultPage( responsePage : ResponsePageSearch<T>)
    {
        this.listaResultado = responsePage.resultSearch;
        this.num_resultados = responsePage.TotalResult;
        this.num_total_paginas = responsePage.TotalPages;
        this.num_pagina_actual = responsePage.Page;
        this.num_resultados_pagina = responsePage.Page;
        this.item_inicio = responsePage.StarResult;
        this.item_fin = responsePage.EndResult;
    }
}