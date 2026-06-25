import { Injectable } from "@angular/core";
import { AppSetting } from "src/app/config/app.setting";
import { RespuestaWsDto } from "../../compartido/entity/RespuestaWsDto";
import { ApiService } from "../../compartido/service/api.service";
import { BusquedaProductoDto } from '../entity/BusquedaProductoDto';
import { Observable } from 'rxjs';
import { HttpClient } from "@angular/common/http";



@Injectable({
    providedIn: 'root'
})

export class PreventaService {
  
  
    constructor(private apiService: ApiService) {
    }

    async BuscarProducto(Request: any)
    {
        let url: string = `${AppSetting.API}/api/v1/productSearch/query`;
        let RespuestaWS : RespuestaWsDto;

        RespuestaWS = await this.apiService.EjecutarServicioPost(url,Request);

        return RespuestaWS;
    }

    async ObtenerProducto(Request: any)
    {
        let url: string = `${AppSetting.API}/api/venta/ObtenerProducto`;
        let RespuestaWS : RespuestaWsDto;

        RespuestaWS = await this.apiService.EjecutarServicioPost(url,Request);

        return RespuestaWS;
    }

    // BuscarProducto(data : any):Observable<any>
    // {
    //     let url: string = `${AppSetting.API}/api/venta/BuscarProducto`;

    //     return this.http.post<any>(url,data);
    // }

}