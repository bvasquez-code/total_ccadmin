import {Injectable} from '@angular/core';
import {HttpClient, HttpHeaders} from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { RespuestaWsDto } from '../entity/RespuestaWsDto';
import { ResponseWsDto } from '../../shared/model/dto/ResponseWsDto';
import { SessionStorageDto } from '../entity/SessionStorageDto';
import { Router } from '@angular/router';
import { catchError, map } from 'rxjs/operators';

@Injectable({
    providedIn: 'root'
})
export class ApiService {

    constructor(
         private http: HttpClient,
         private router: Router,
    ){}

    generarheaders()
    {
        if( !sessionStorage.getItem('Token')){
            this.router.navigate(['/login']);
            return;
        }
        let DataToken :string | null = sessionStorage.getItem('Token');

        let Token  : string = "";

        if( DataToken != null)
        {
            Token = DataToken;
        }
        else
        {
            Token = "";
        }

        const Myheaders = { 
            'Content-Type': 'application/json', 
            'timeout': '3600000',
            'Access-Control-Allow-Origin': '*',
            'Access-Control-Allow-Methods': '*',
            'Access-Control-Allow-Headers' : 'Origin, Accept, Accept-Version, Content-Length, Content-MD5, Content-Type, Date, X-Api-Version, X-Response-Time, X-PINGOTHER, X-CSRF-Token,Authorization',
            'Authorization' : Token
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
        let token : any = "";
        this.http
            .post<any>(URL, Request,{observe: 'response'})
            .subscribe(resp => {

                console.log({ resp : resp } )

                token = resp.headers.get('Authorization');
                
                sessionStorage.setItem('Token', token);

                this.InvokeGetService(URLDataLogin,{})
                .toPromise()
                .then(data => { 

                    let rpt : ResponseWsDto = data;

                    console.log( data );

                    if( !rpt.ErrorStatus )
                    {
                        let sessionStorageDto : SessionStorageDto = new SessionStorageDto();

                        sessionStorageDto = rpt.Data;

                        sessionStorage.setItem('UserCod', sessionStorageDto.UserCod);
                        sessionStorage.setItem('PersonCod', sessionStorageDto.PersonCod);
                        sessionStorage.setItem('Email', sessionStorageDto.Email);
                        sessionStorage.setItem('SessionID',sessionStorageDto.SessionID.toString());
                        sessionStorage.setItem('Names', sessionStorageDto.Names);
                        sessionStorage.setItem('StoreCod', sessionStorageDto.StoreCod);
                        sessionStorage.setItem('AppMenuPermissions', JSON.stringify(sessionStorageDto.AppMenuPermissions));
                        location.reload();  
                    }

                }).catch( function(e){
                    alert("Error en el servicio :"+e.error.mensaje);
                });

            });
    }

}
