import { DefaultUrlSerializer, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { DataSesionService } from '../../compartido/service/datasesion.service';
import { SessionStorageDto } from '../../compartido/entity/SessionStorageDto';
import { ResponseWsDto } from '../../shared/model/dto/ResponseWsDto';
import { ApplicationInitializationGuard } from './application-initialization.guard';
import {
  ApplicationInitializationService,
  ApplicationInitializationStatusDto
} from './application-initialization.service';

describe('ApplicationInitializationGuard', () => {
  const initializationUrl = '/enterprise/system/pages/applicationinitialization';
  let dataSesionService: DataSesionService;
  let initializationService: jasmine.SpyObj<ApplicationInitializationService>;
  let guard: ApplicationInitializationGuard;

  function saveSession(required = false, userCod = 'ROOT'): void {
    const session = new SessionStorageDto();
    session.UserCod = userCod;
    session.StoreCod = 'T001';
    session.ApplicationInitializationRequired = required;
    dataSesionService.SaveSession('fixture-token', session);
  }

  function status(companyPending: boolean, storePending: boolean): void {
    const response = new ResponseWsDto();
    response.Data = {
      Required: companyPending || storePending,
      CompanyPending: companyPending,
      StorePending: storePending,
      DefaultStoreCod: storePending ? 'T001' : ''
    } as ApplicationInitializationStatusDto;
    initializationService.findStatus.and.resolveTo(response);
  }

  function navigate(url: string): Promise<boolean | UrlTree> {
    return guard.canActivateChild({}, { url } as RouterStateSnapshot);
  }

  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    dataSesionService = new DataSesionService();
    initializationService = jasmine.createSpyObj('ApplicationInitializationService', ['findStatus']);
    const router = jasmine.createSpyObj<Router>('Router', ['parseUrl']);
    router.parseUrl.and.callFake(url => new DefaultUrlSerializer().parse(url));
    guard = new ApplicationInitializationGuard(dataSesionService, initializationService, router);
    saveSession();
  });

  afterEach(() => {
    localStorage.clear();
    sessionStorage.clear();
  });

  it('bloquea una ruta directa tras reiniciar la base aunque el navegador recuerde que estaba configurada', async () => {
    status(true, true);
    const result = await navigate('/enterprise/sale/pages/createsale');
    expect(new DefaultUrlSerializer().serialize(result as UrlTree)).toBe(initializationUrl);
    expect(dataSesionService.RequiresApplicationInitialization()).toBeTrue();
    expect(localStorage.getItem('CompanyInitializationPending')).toBe('true');
    expect(localStorage.getItem('StoreInitializationPending')).toBe('true');
    expect(localStorage.getItem('DefaultStoreCod')).toBe('T001');
  });

  for (const pending of [[true, false], [false, true]]) {
    it(`mantiene el bloqueo con compañía pendiente ${pending[0]} y tienda pendiente ${pending[1]}`, async () => {
      status(pending[0], pending[1]);
      expect(await navigate('/')).toEqual(new DefaultUrlSerializer().parse(initializationUrl));
    });
  }

  it('permite el asistente cuando hay configuración pendiente', async () => {
    status(true, true);
    expect(await navigate(initializationUrl)).toBeTrue();
  });

  it('habilita las rutas cuando el backend confirma compañía y tienda configuradas', async () => {
    saveSession(true);
    status(false, false);
    expect(await navigate('/enterprise/sale/pages/createsale')).toBeTrue();
    expect(dataSesionService.RequiresApplicationInitialization()).toBeFalse();
    expect(localStorage.getItem('ApplicationInitializationRequired')).toBe('false');
  });

  it('permite continuar los pasos opcionales después de configurar compañía y tienda', async () => {
    sessionStorage.setItem('ApplicationInitializationStep', '2');
    status(false, false);
    expect(await navigate(initializationUrl)).toBeTrue();
  });

  it('no permite acceder si falla la comprobación del backend, aunque el estado local sea completado', async () => {
    const response = new ResponseWsDto();
    response.ErrorStatus = true;
    initializationService.findStatus.and.resolveTo(response);
    expect(await navigate('/enterprise/sale/pages/createsale')).toBeFalse();
  });

  it('no permite acceder si la comprobación no devuelve datos', async () => {
    initializationService.findStatus.and.resolveTo(new ResponseWsDto());
    expect(await navigate('/')).toBeFalse();
  });

  it('evita notificar cambios entre pestañas cuando el estado sigue siendo el mismo', async () => {
    status(true, true);
    await navigate(initializationUrl);
    const synchronization = localStorage.getItem('CcAdminSessionSynchronization');
    await navigate(initializationUrl);
    expect(localStorage.getItem('CcAdminSessionSynchronization')).toBe(synchronization);
  });

  it('mantiene el acceso habitual para usuarios distintos de ROOT', async () => {
    saveSession(false, 'ADMIN');
    expect(await navigate('/')).toBeTrue();
    expect(initializationService.findStatus).not.toHaveBeenCalled();
  });
});
