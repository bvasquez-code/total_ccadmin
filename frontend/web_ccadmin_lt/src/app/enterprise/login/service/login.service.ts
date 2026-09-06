import { Injectable } from "@angular/core";
import { AppSetting } from "src/app/config/app.setting";
import { ApiService } from "../../compartido/service/api.service";



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


        return this.apiService.ExecutePostServiceLogin(url,{
            UserCod : UserCod,
            Password : Password
        },url2);


    }


}