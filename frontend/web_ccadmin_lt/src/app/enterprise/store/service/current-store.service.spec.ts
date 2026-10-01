import { ToastrService } from 'ngx-toastr';
import { SessionStorageDto } from '../../compartido/entity/SessionStorageDto';
import { DataSesionService } from '../../compartido/service/datasesion.service';
import { ResponseWsDto } from '../../shared/model/dto/ResponseWsDto';
import { CurrentStoreService } from './current-store.service';
import { StoreService } from './store.service';

describe('CurrentStoreService', () => {
  let storeService: jasmine.SpyObj<StoreService>;
  let toastrService: jasmine.SpyObj<ToastrService>;
  let dataSesionService: DataSesionService;
  let service: CurrentStoreService;

  function response(storeName: string): ResponseWsDto {
    return Object.assign(new ResponseWsDto(), { Data: { Name: storeName } });
  }

  function saveSession(sessionId = 1, storeCode = 'T001'): void {
    dataSesionService.SaveSession('token-' + sessionId, Object.assign(new SessionStorageDto(), {
      SessionID: sessionId, UserCod: 'ANA01', StoreCod: storeCode
    }));
  }

  function createService(): CurrentStoreService {
    return new CurrentStoreService(storeService, dataSesionService, toastrService);
  }

  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    dataSesionService = new DataSesionService();
    storeService = jasmine.createSpyObj<StoreService>('StoreService', ['FindById']);
    toastrService = jasmine.createSpyObj<ToastrService>('ToastrService', ['error']);
    storeService.FindById.and.returnValue(Promise.resolve(response(' Tienda central ')));
    saveSession();
    service = createService();
  });

  afterEach(() => {
    localStorage.clear();
    sessionStorage.clear();
  });

  it('comparte la petición en curso y consulta una sola vez en la misma sesión', async () => {
    const firstRequest = service.getCurrentStoreName();
    const secondRequest = service.getCurrentStoreName();
    expect(firstRequest).toBe(secondRequest);
    expect(await firstRequest).toBe('Tienda central');
    expect(await service.getCurrentStoreName()).toBe('Tienda central');
    expect(storeService.FindById).toHaveBeenCalledOnceWith('T001');
  });

  it('reutiliza el nombre después de recrear los servicios al recargar la página', async () => {
    await service.getCurrentStoreName();
    dataSesionService = new DataSesionService();
    expect(await createService().getCurrentStoreName()).toBe('Tienda central');
    expect(storeService.FindById).toHaveBeenCalledTimes(1);
  });

  it('vuelve a consultar al iniciar otra sesión aunque sea el mismo usuario y tienda', async () => {
    await service.getCurrentStoreName();
    saveSession(2);
    storeService.FindById.and.returnValue(Promise.resolve(response('Nombre actualizado')));
    expect(await service.getCurrentStoreName()).toBe('Nombre actualizado');
    expect(storeService.FindById).toHaveBeenCalledTimes(2);
  });

  it('limpia el nombre al salir y no consulta sin sesión', async () => {
    await service.getCurrentStoreName();
    dataSesionService.ClearSession();
    expect(await service.getCurrentStoreName()).toBe('');
    expect(sessionStorage.getItem('CurrentStoreName')).toBeNull();
    saveSession(2, 'T002');
    await service.getCurrentStoreName();
    expect(storeService.FindById.calls.allArgs()).toEqual([['T001'], ['T002']]);
  });

  it('descarta respuestas tardías de una sesión anterior', async () => {
    let resolvePrevious!: (value: ResponseWsDto) => void;
    storeService.FindById.and.returnValue(new Promise(resolve => resolvePrevious = resolve));
    const previousRequest = service.getCurrentStoreName();
    saveSession(2, 'T002');
    storeService.FindById.and.returnValue(Promise.resolve(response('Tienda nueva')));
    expect(await service.getCurrentStoreName()).toBe('Tienda nueva');
    resolvePrevious(response('Tienda anterior'));
    expect(await previousRequest).toBe('');
    expect(await createService().getCurrentStoreName()).toBe('Tienda nueva');
    expect(storeService.FindById).toHaveBeenCalledTimes(2);
  });

  it('muestra el error del servicio una vez y evita reintentos automáticos', async () => {
    storeService.FindById.and.returnValue(Promise.resolve(Object.assign(new ResponseWsDto(), {
      ErrorStatus: true, Message: 'Tienda no disponible'
    })));
    expect(await service.getCurrentStoreName()).toBe('');
    expect(await createService().getCurrentStoreName()).toBe('');
    expect(storeService.FindById).toHaveBeenCalledTimes(1);
    expect(toastrService.error).toHaveBeenCalledOnceWith('Tienda no disponible');
    saveSession(2);
    storeService.FindById.and.returnValue(Promise.resolve(response('Tienda disponible')));
    expect(await service.getCurrentStoreName()).toBe('Tienda disponible');
    expect(storeService.FindById).toHaveBeenCalledTimes(2);
  });

  it('maneja los fallos de conexión sin repetir la consulta', async () => {
    storeService.FindById.and.callFake(() => Promise.reject(new Error('Sin conexión')));
    expect(await service.getCurrentStoreName()).toBe('');
    expect(await service.getCurrentStoreName()).toBe('');
    expect(storeService.FindById).toHaveBeenCalledTimes(1);
    expect(toastrService.error).toHaveBeenCalledOnceWith('Sin conexión');
  });

  it('no consulta si la sesión aún no tiene tienda', async () => {
    saveSession(1, '');
    expect(await service.getCurrentStoreName()).toBe('');
    expect(storeService.FindById).not.toHaveBeenCalled();
  });

  it('conserva un nombre vacío sin repetir la consulta', async () => {
    storeService.FindById.and.returnValue(Promise.resolve(response('')));
    expect(await service.getCurrentStoreName()).toBe('');
    expect(await createService().getCurrentStoreName()).toBe('');
    expect(storeService.FindById).toHaveBeenCalledTimes(1);
  });

  it('recupera una caché inválida con una única consulta', async () => {
    sessionStorage.setItem('CurrentStoreName', 'contenido inválido');
    expect(await service.getCurrentStoreName()).toBe('Tienda central');
    expect(await service.getCurrentStoreName()).toBe('Tienda central');
    expect(storeService.FindById).toHaveBeenCalledTimes(1);
  });
});
