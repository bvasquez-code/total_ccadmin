import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { DataSesionService } from '../../../compartido/service/datasesion.service';
import { SessionStorageDto } from '../../../compartido/entity/SessionStorageDto';
import { StoreEntity } from '../../../shared/model/entity/StoreEntity';
import { ApplicationInitializationComponent } from './applicationinitialization.component';

describe('ApplicationInitializationComponent progress', () => {
  let dataSesionService: DataSesionService;
  let router: jasmine.SpyObj<Router>;

  function createComponent(companyPending: boolean, storePending: boolean, savedStep?: number): ApplicationInitializationComponent {
    const session = new SessionStorageDto();
    session.UserCod = 'ROOT';
    session.StoreCod = 'T001';
    session.ApplicationInitializationRequired = companyPending || storePending;
    session.CompanyInitializationPending = companyPending;
    session.StoreInitializationPending = storePending;
    dataSesionService.SaveSession('fixture-token', session);
    if (savedStep !== undefined) {
      sessionStorage.setItem('ApplicationInitializationStep', String(savedStep));
    }
    const toastr = jasmine.createSpyObj<ToastrService>('ToastrService', ['info']);
    const component = new ApplicationInitializationComponent(dataSesionService, router, toastr);
    component.ngOnInit();
    return component;
  }

  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    dataSesionService = new DataSesionService();
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    spyOn(window, 'scrollTo');
  });

  afterEach(() => {
    localStorage.clear();
    sessionStorage.clear();
  });

  it('inicia en tienda cuando solo falta configurarla y no hay progreso guardado', () => {
    expect(createComponent(false, true).CurrentStep).toBe(1);
  });

  it('vuelve a compañía si se reinició la base aunque antes estuviera en producto', () => {
    expect(createComponent(true, true, 3).CurrentStep).toBe(0);
  });

  it('mantiene los menús bloqueados después de compañía si la tienda todavía está pendiente', () => {
    const component = createComponent(true, true, 3);
    component.companyConfigured();
    expect(component.CurrentStep).toBe(1);
    expect(dataSesionService.RequiresApplicationInitialization()).toBeTrue();
    component.storeConfigured({ StoreCod: 'T001' } as StoreEntity);
    expect(component.CurrentStep).toBe(2);
    expect(dataSesionService.RequiresApplicationInitialization()).toBeFalse();
  });

  it('retoma administrador cuando compañía y tienda ya están configuradas', () => {
    expect(createComponent(false, false, 2).CurrentStep).toBe(2);
  });

  it('no interpreta la ausencia de progreso como un paso cero guardado', () => {
    createComponent(false, false);
    expect(router.navigate).toHaveBeenCalledWith(['/']);
  });

  it('pasa por talonarios antes de permitir crear u omitir productos', () => {
    const component = createComponent(false, false, 2);
    component.administratorConfigured();
    expect(component.CurrentStep).toBe(3);
    expect(component.Steps[3].description).toBe('Talonarios y series');
    component.skipProductConfiguration();
    expect(component.CurrentStep).toBe(3);
    expect(router.navigate).not.toHaveBeenCalled();
    component.counterfoilsConfigured();
    expect(component.CurrentStep).toBe(4);
    expect(component.Steps[4].title).toBe('SUNAT');
    component.skipProductConfiguration();
    expect(component.CurrentStep).toBe(4);
    component.sunatConfigured();
    expect(component.CurrentStep).toBe(component.ProductStep);
    expect(sessionStorage.getItem('ApplicationInitializationStep')).toBe('5');
    component.skipProductConfiguration();
    expect(sessionStorage.getItem('ApplicationInitializationStep')).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/enterprise/product/pages/listProduct']);
  });

  it('retoma talonarios, SUNAT o producto después de recargar', () => {
    expect(createComponent(false, false, 3).CurrentStep).toBe(3);
    expect(createComponent(false, false, 4).CurrentStep).toBe(4);
    const component = createComponent(false, false, 5);
    expect(component.CurrentStep).toBe(component.ProductStep);
  });
});
