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
    private readonly sessionKeys: string[] = [
        'Token',
        'UserCod',
        'PersonCod',
        'Email',
        'SessionID',
        'StoreCod',
        'Names',
        'AppMenuPermissions',
        'ApplicationInitializationRequired',
        'CompanyInitializationPending',
        'StoreInitializationPending',
        'DefaultStoreCod'
    ];
    private readonly sessionSynchronizationKey: string = 'CcAdminSessionSynchronization';

    constructor()
    {
        this.cargarInfoSesion();
    }

    private cargarInfoSesion()
    {
        this.sessionStorageDto.Token = this.ObtenerKeySesion( localStorage.getItem('Token') );
        this.sessionStorageDto.UserCod = this.ObtenerKeySesion( localStorage.getItem('UserCod') );
        this.sessionStorageDto.PersonCod = this.ObtenerKeySesion( localStorage.getItem('PersonCod') );
        this.sessionStorageDto.Email = this.ObtenerKeySesion( localStorage.getItem('Email') );
        this.sessionStorageDto.SessionID = Number(this.ObtenerKeySesion( localStorage.getItem('SessionID') ));
        this.sessionStorageDto.StoreCod = this.ObtenerKeySesion( localStorage.getItem('StoreCod') );
        this.sessionStorageDto.Names = this.ObtenerKeySesion( localStorage.getItem('Names') );
        this.sessionStorageDto.AppMenuPermissions = this.obtenerPermisosMenuSesion();
        this.sessionStorageDto.ApplicationInitializationRequired =
            localStorage.getItem('ApplicationInitializationRequired') === 'true';
        this.sessionStorageDto.CompanyInitializationPending =
            localStorage.getItem('CompanyInitializationPending') === 'true';
        this.sessionStorageDto.StoreInitializationPending =
            localStorage.getItem('StoreInitializationPending') === 'true';
        this.sessionStorageDto.DefaultStoreCod =
            this.ObtenerKeySesion(localStorage.getItem('DefaultStoreCod'));
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

    SessionExists(): boolean
    {
        const token = this.GetToken().toLowerCase();
        return !!token && token !== 'null' && token !== 'undefined';
    }

    GetToken(): string
    {
        return this.ObtenerKeySesion(localStorage.getItem('Token')).trim();
    }

    SaveSession(token: string, session: SessionStorageDto): void
    {
        localStorage.setItem('UserCod', session.UserCod || '');
        localStorage.setItem('PersonCod', session.PersonCod || '');
        localStorage.setItem('Email', session.Email || '');
        localStorage.setItem('SessionID', (session.SessionID || 0).toString());
        localStorage.setItem('Names', session.Names || '');
        localStorage.setItem('StoreCod', session.StoreCod || '');
        localStorage.setItem('AppMenuPermissions', JSON.stringify(session.AppMenuPermissions || []));
        localStorage.setItem(
            'ApplicationInitializationRequired',
            String(Boolean(session.ApplicationInitializationRequired))
        );
        localStorage.setItem(
            'CompanyInitializationPending',
            String(Boolean(session.CompanyInitializationPending))
        );
        localStorage.setItem(
            'StoreInitializationPending',
            String(Boolean(session.StoreInitializationPending))
        );
        localStorage.setItem('DefaultStoreCod', session.DefaultStoreCod || '');
        localStorage.setItem('Token', token);

        this.ClearCurrentTabData();
        this.cargarInfoSesion();
        this.notifySessionChange();
    }

    ClearSession(): void
    {
        this.sessionKeys.forEach(key => localStorage.removeItem(key));
        this.ClearCurrentTabData();
        this.cargarInfoSesion();
        this.notifySessionChange();
    }

    ClearCurrentTabData(): void
    {
        sessionStorage.clear();
    }

    ReloadSession(): void
    {
        this.cargarInfoSesion();
    }

    IsSessionSynchronizationEvent(event: StorageEvent): boolean
    {
        return event.storageArea === localStorage && event.key === this.sessionSynchronizationKey;
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

    RequiresApplicationInitialization(): boolean
    {
        return this.sessionStorageDto.UserCod.toUpperCase() === 'ROOT'
            && this.sessionStorageDto.ApplicationInitializationRequired;
    }

    CompleteApplicationInitialization(): void
    {
        localStorage.setItem('ApplicationInitializationRequired', 'false');
        localStorage.setItem('CompanyInitializationPending', 'false');
        localStorage.setItem('StoreInitializationPending', 'false');
        localStorage.removeItem('DefaultStoreCod');
        this.cargarInfoSesion();
        this.notifySessionChange();
    }

    private obtenerPermisosMenuSesion(): AppMenuEntity[]
    {
        const appMenuPermissions = this.ObtenerKeySesion(localStorage.getItem('AppMenuPermissions'));

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

    private notifySessionChange(): void
    {
        const synchronizationValue = `${Date.now()}-${Math.random()}`;
        localStorage.setItem(this.sessionSynchronizationKey, synchronizationValue);
    }

}
