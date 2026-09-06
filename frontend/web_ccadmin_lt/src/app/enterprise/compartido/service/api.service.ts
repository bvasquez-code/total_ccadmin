import { AlertService } from '../../shared/service/AlertService';
import { ToastrService } from 'ngx-toastr';
import { AppSetting } from 'src/app/config/app.setting';
import {Injectable} from '@angular/core';
import {HttpClient, HttpHeaders} from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { RespuestaWsDto } from '../entity/RespuestaWsDto';
import { ResponseWsDto } from '../../shared/model/dto/ResponseWsDto';
import { SessionStorageDto } from '../entity/SessionStorageDto';
import { Router } from '@angular/router';
import { catchError, map } from 'rxjs/operators';
import { DataSesionService } from './datasesion.service';

@Injectable({
    providedIn: 'root'
})
export class ApiService {

    constructor(
         private http: HttpClient,
         private alertService: AlertService,
         private toastrService: ToastrService,
         private router: Router,
         private dataSesionService: DataSesionService,
    ){}

    generarheaders()
    {
        const token = this.dataSesionService.GetToken();

        if (!token) {
            this.router.navigate(['/login']);
        }

        return this.createHeaders(token);
    }

    private createHeaders(token: string)
    {
        const Myheaders = { 
            'Content-Type': 'application/json', 
            'timeout': '3600000',
            'Access-Control-Allow-Origin': '*',
            'Access-Control-Allow-Methods': '*',
            'Access-Control-Allow-Headers' : 'Origin, Accept, Accept-Version, Content-Length, Content-MD5, Content-Type, Date, X-Api-Version, X-Response-Time, X-PINGOTHER, X-CSRF-Token,Authorization',
            'Authorization' : token
        };

        return Myheaders;
    }


    public EjecutarServicioGet(URL: string): Observable<any> {

        return this.http.get<any>(URL,{
            headers: new HttpHeaders(this.generarheaders())
        });
    }

    public ConsultarServicioGet(URL: string): Observable<any> {

        return this.http.get<any>(URL,{
            headers: new HttpHeaders(this.generarheaders())
        });
    }

    public ConsultarServicioPost(URL: string, Request : any): Observable<any> {

        return this.http.post<any>(URL, Request, {
            headers: new HttpHeaders(this.generarheaders())
        });
    }


    async EjecutarServicioPost(URL: string, Request : any)
    {
        let RespuestaWS : RespuestaWsDto = new RespuestaWsDto();

        await this.ConsultarServicioPost(URL,Request)
        .toPromise()
        .then(data => { 
            console.log( data );
            RespuestaWS = data;
        }).catch( function(e){
            alert("Error en el servicio :"+e.error.mensaje);
            RespuestaWS = new RespuestaWsDto();
            RespuestaWS.cargarError(e);
            RespuestaWS.mensaje = e.error.mensaje;
            console.log({ ERROR : e });
        });
        return RespuestaWS;
    }

    ExecuteGetService2(URL: string, Request: any): Observable<ResponseWsDto> {
        return this.InvokeGetService(URL, Request).pipe(
            map(data => {
                let respuestaWS: ResponseWsDto = new ResponseWsDto();
                respuestaWS = data;
                return respuestaWS;
            }),
            catchError(error => {
                console.log({ ERROR: error });
                let respuestaWS: ResponseWsDto = new ResponseWsDto();
                respuestaWS.addError(error);
                respuestaWS.Message = error.error.mensaje;
                return [respuestaWS];
            })
        );
    }

    public InvokePostService(URL: string, Request : any): Observable<any> {

        return this.http.post<any>(URL, Request, {
            headers: new HttpHeaders(this.generarheaders())
        });
    }

    public InvokePostFormDataService(URL: string, Request: FormData): Observable<any> {
        const token = this.dataSesionService.GetToken();
        if (!token) {
            this.router.navigate(['/login']);
        }
        return this.http.post<any>(URL, Request, {
            headers: new HttpHeaders({ 'Authorization': token })
        });
    }

    public InvokeGetService(URL: string,Request : any): Observable<any> {

        let URLparam : string = new URLSearchParams(Request).toString();

        return this.http.get<any>(URL +"?"+ URLparam,{
            headers: new HttpHeaders(this.generarheaders())
        });
    }

    public InvokeDeleteService(URL: string): Observable<any> {

        return this.http.delete<any>(URL, {
            headers: new HttpHeaders(this.generarheaders())
        });
    }

    async ExecutePostService(URL: string, Request : any)
    {
        let RespuestaWS : ResponseWsDto = new ResponseWsDto();

        await this.InvokePostService(URL,Request)
        .toPromise()
        .then(data => { 
            RespuestaWS = data;
        }).catch( function(e){
            RespuestaWS = e.error;
            console.log({ ERROR : e });
        });
        return RespuestaWS;
    }

    async ExecutePostFormDataService(URL: string, Request: FormData)
    {
        let RespuestaWS : ResponseWsDto = new ResponseWsDto();

        await this.InvokePostFormDataService(URL, Request)
        .toPromise()
        .then(data => {
            RespuestaWS = data;
        }).catch(function(e){
            RespuestaWS = e.error || new ResponseWsDto();
            RespuestaWS.ErrorStatus = true;
            RespuestaWS.Message = RespuestaWS.Message || 'No fue posible enviar el archivo';
            console.log({ ERROR : e });
        });
        return RespuestaWS;
    }

    async ExecuteGetService(URL: string, Request : any)
    {
        let RespuestaWS : ResponseWsDto = new ResponseWsDto();

        await this.InvokeGetService(URL,Request)
        .toPromise()
        .then(data => { 
            RespuestaWS = data;
        }).catch( function(e){
            // alert("Error en el servicio :"+e.error.mensaje);
            RespuestaWS = new ResponseWsDto();
            RespuestaWS.addError(e);
            RespuestaWS.Message = e.error.mensaje;
            console.log({ ERROR : e });
        });
        return RespuestaWS;
    }

    async ExecuteDeleteService(URL: string)
    {
        let RespuestaWS : ResponseWsDto = new ResponseWsDto();

        await this.InvokeDeleteService(URL)
        .toPromise()
        .then(data => {
            RespuestaWS = data;
        }).catch( function(e){
            RespuestaWS = new ResponseWsDto();
            RespuestaWS.addError(e);
            RespuestaWS.Message = e.error.mensaje;
            console.log({ ERROR : e });
        });
        return RespuestaWS;
    }

    async ExecutePostServiceLogin(URL: string, Request : any, URLDataLogin : string)
    {
        try {
            const response = await this.http.post(URL, Request, { observe: 'response' }).toPromise();
            const token = response?.headers.get('Authorization') || '';
            if (!token) throw new Error('No se recibio una sesion valida');
            const headers = new HttpHeaders(this.createHeaders(token));
            const result = await this.http.get<ResponseWsDto>(URLDataLogin, { headers }).toPromise();
            if (!result || result.ErrorStatus) throw new Error(result?.Message || 'No se pudo cargar la sesion');
            let session: SessionStorageDto = result.Data;
            if ((session.StoreList || []).length > 1) {
                const selection = await this.alertService.selectStore(session.StoreList, async storeCod => {
                    try {
                        const selected = await this.http.post<ResponseWsDto>(
                            `${AppSetting.API}/api/v1/security/selectStore`, { StoreCod: storeCod }, { headers }
                        ).toPromise();
                        if (!selected || selected.ErrorStatus) return false;
                        session = selected.Data;
                        return true;
                    } catch { return false; }
                });
                if (!selection.isConfirmed) return;
            }
            if (!session.StoreCod && !session.ApplicationInitializationRequired) {
                throw new Error('El usuario no tiene tiendas asignadas. Contacta al administrador.');
            }
            this.dataSesionService.SaveSession(token, session);
            window.location.replace(this.dataSesionService.RequiresApplicationInitialization()
                ? '/enterprise/system/pages/applicationinitialization' : '/');
        } catch (error: any) {
            this.toastrService.error(error?.error?.Message || error.message || 'No se pudo iniciar sesion');
        }
    }

}
