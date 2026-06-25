import { Injectable } from "@angular/core";
import { AppSetting } from "src/app/config/app.setting";
import { RespuestaWsDto } from "../../compartido/entity/RespuestaWsDto";
import { ApiService } from "../../compartido/service/api.service";
import { Observable } from 'rxjs';
import { HttpClient } from "@angular/common/http";



@Injectable({
    providedIn: 'root'
})

export class LoginService {
  
  
    constructor(private apiService: ApiService) {
    }

    IniciarSesion(UserCod : string, Password : string)
    {
        let url: string = `${AppSetting.API}/login`;
        let url2: string = `${AppSetting.API}/api/v1/security/findUserSession`;

        let RespuestaWS : RespuestaWsDto = new RespuestaWsDto();

        this.apiService.ExecutePostServiceLogin(url,{
            UserCod : UserCod,
            Password : Password
        },url2);

        return RespuestaWS;
    }


}