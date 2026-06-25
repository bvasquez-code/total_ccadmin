import {Injectable} from '@angular/core';
import {HttpClient, HttpHeaders} from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { RespuestaWsDto } from '../entity/RespuestaWsDto';
import { SessionStorageDto } from '../entity/SessionStorageDto';
import { AppMenuEntity } from '../../menu/model/entity/AppMenuEntity';


@Injectable({
    providedIn: 'root'
})
export class DataSesionService {

    private sessionStorageDto : SessionStorageDto = new SessionStorageDto();

    constructor()
    {
        this.cargarInfoSesion();
    }

    private cargarInfoSesion()
    {
        this.sessionStorageDto.Token = this.ObtenerKeySesion( sessionStorage.getItem('Token') );
        this.sessionStorageDto.UserCod = this.ObtenerKeySesion( sessionStorage.getItem('UserCod') );
        this.sessionStorageDto.PersonCod = this.ObtenerKeySesion( sessionStorage.getItem('PersonCod') );
        this.sessionStorageDto.Email = this.ObtenerKeySesion( sessionStorage.getItem('Email') );
        this.sessionStorageDto.SessionID = Number(this.ObtenerKeySesion( sessionStorage.getItem('SessionID') ));
        this.sessionStorageDto.StoreCod = this.ObtenerKeySesion( sessionStorage.getItem('StoreCod') );
        this.sessionStorageDto.Names = this.ObtenerKeySesion( sessionStorage.getItem('Names') );
        this.sessionStorageDto.AppMenuPermissions = this.obtenerPermisosMenuSesion();
    }

    private ObtenerKeySesion( valor : any ) : string
    {
        if( valor)
        {
            return valor;
        }

        return "";
    }

    getSessionStorageDto()
    {
        return this.sessionStorageDto;
    }

    PermissionExists(MenuCod : string):boolean
    {
        let AppMenuPermissions : AppMenuEntity[] = this.getSessionStorageDto().AppMenuPermissions || [];
        if(AppMenuPermissions.find( e => e.MenuCod === MenuCod )){
            return true;
        }else{
            return false;
        }
    }

    private obtenerPermisosMenuSesion(): AppMenuEntity[]
    {
        const appMenuPermissions = this.ObtenerKeySesion(sessionStorage.getItem('AppMenuPermissions'));

        if (!appMenuPermissions) {
            return [];
        }

        try {
            const permisos = JSON.parse(appMenuPermissions);
            return Array.isArray(permisos) ? permisos : [];
        } catch (error) {
            return [];
        }
    }

}
