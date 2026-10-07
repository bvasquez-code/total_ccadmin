import { CommonModule } from '@angular/common';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';
import { AppComponent } from './app.component';
import { DataSesionService } from './enterprise/compartido/service/datasesion.service';
import { SessionStorageDto } from './enterprise/compartido/entity/SessionStorageDto';
import { SpinnerService } from './enterprise/shared/service/spinner.service';

describe('AppComponent initialization menus', () => {
  let fixture: ComponentFixture<AppComponent>;
  let dataSesionService: DataSesionService;

  beforeEach(async () => {
    localStorage.clear();
    sessionStorage.clear();
    dataSesionService = new DataSesionService();
    const session = new SessionStorageDto();
    session.UserCod = 'ROOT';
    session.ApplicationInitializationRequired = true;
    session.CompanyInitializationPending = true;
    session.StoreInitializationPending = true;
    dataSesionService.SaveSession('fixture-token', session);
    await TestBed.configureTestingModule({
      imports: [CommonModule, RouterTestingModule],
      declarations: [AppComponent],
      providers: [
        { provide: DataSesionService, useValue: dataSesionService },
        { provide: SpinnerService, useValue: { IsLoading$: of(false) } }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
    fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.clear();
    sessionStorage.clear();
  });

  it('oculta los menús mientras falta compañía o tienda, incluso si ROOT tiene permisos', () => {
    expect(fixture.nativeElement.querySelector('app-menusidebar')).toBeNull();
    dataSesionService.UpdateApplicationInitializationStatus({
      Required: true, CompanyPending: false, StorePending: true, DefaultStoreCod: 'T001'
    });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-menusidebar')).toBeNull();
  });

  it('muestra los menús al completar compañía y tienda sin recargar el navegador', () => {
    dataSesionService.CompleteApplicationInitialization();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-menusidebar')).not.toBeNull();
  });
});
