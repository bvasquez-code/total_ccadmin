import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ToastrService } from 'ngx-toastr';
import { ResponseWsDto } from '../../../shared/model/dto/ResponseWsDto';
import { SunatInitializationForm } from '../../model/SunatInitializationModels';
import { SunatInitializationService } from '../../service/sunat-initialization.service';
import { ConfigureSunatComponent } from './configuresunat.component';

describe('ConfigureSunatComponent', () => {
  let fixture: ComponentFixture<ConfigureSunatComponent>;
  let component: ConfigureSunatComponent;
  let service: jasmine.SpyObj<SunatInitializationService>;
  let toastr: jasmine.SpyObj<ToastrService>;
  let form: SunatInitializationForm;

  beforeEach(async () => {
    form = {
      Mode: 'NONE', IssuerRuc: '20123456789', SolUser: '', HasSolPassword: false,
      HasCertificate: false, HasCertificatePassword: false, GuideEnabled: false,
      GuideClientId: '', HasGuideClientSecret: false, BetaEndpoint: 'https://beta.example/billService',
      ProductionEndpoint: 'https://production.example/billService', GuideEndpoint: 'https://guide.example', GuideTokenEndpoint: 'https://token.example'
    };
    service = jasmine.createSpyObj('SunatInitializationService', ['findDataForm', 'configure']);
    service.findDataForm.and.resolveTo({ ErrorStatus: false, Data: form } as ResponseWsDto);
    service.configure.and.resolveTo({ ErrorStatus: false, Data: form } as ResponseWsDto);
    toastr = jasmine.createSpyObj('ToastrService', ['success', 'error']);
    await TestBed.configureTestingModule({ declarations: [ConfigureSunatComponent], imports: [CommonModule, FormsModule],
      providers: [{ provide: SunatInitializationService, useValue: service }, { provide: ToastrService, useValue: toastr }]
    }).compileComponents();
    fixture = TestBed.createComponent(ConfigureSunatComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  });

  it('ofrece las tres opciones y muestra producción solo cuando se selecciona', async () => {
    const select: HTMLSelectElement = fixture.nativeElement.querySelector('#sunatMode');
    expect(Array.from(select.options).map(option => option.value)).toEqual(['NONE', 'BETA', 'PRODUCCION']);
    expect(fixture.nativeElement.querySelector('#sunatCertificate')).toBeNull();
    select.value = 'PRODUCCION';
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('#sunatCertificate')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('#sunatIssuerRuc').value).toBe(form.IssuerRuc);
  });

  it('confirma no usar y solo avanza cuando el backend confirma la desactivación', async () => {
    spyOn(component.ConfigurationCompleted, 'emit');
    await component.confirm();
    expect(service.configure).toHaveBeenCalledWith({ Mode: 'NONE' }, undefined);
    expect(component.ConfigurationCompleted.emit).toHaveBeenCalledTimes(1);
  });

  it('usa pruebas sin enviar credenciales ni certificados de producción', async () => {
    component.Mode = 'BETA';
    component.SolPassword = 'private-secret';
    component.Certificate = new File(['test'], 'private.pfx');
    await component.confirm();
    expect(service.configure).toHaveBeenCalledWith({ Mode: 'BETA' }, undefined);
    expect(component.SolPassword).toBe('');
    expect(component.Certificate).toBeUndefined();
  });

  it('bloquea producción si no se cargó certificado', async () => {
    component.Mode = 'PRODUCCION';
    component.SolUser = 'SOLUSER';
    component.SolPassword = 'secret';
    await component.confirm();
    expect(service.configure).not.toHaveBeenCalled();
    expect(toastr.error).toHaveBeenCalled();
  });

  it('envía el pfx con sus credenciales y limpia los secretos después de guardar', async () => {
    component.Mode = 'PRODUCCION';
    component.SolUser = ' SOLUSER ';
    component.SolPassword = 'sol-secret';
    component.CertificatePassword = 'certificate-secret';
    const certificate = new File(['test'], 'production.pfx');
    component.Certificate = certificate;
    await component.confirm();
    const [configuration, uploaded] = service.configure.calls.mostRecent().args;
    expect(configuration).toEqual(jasmine.objectContaining({ Mode: 'PRODUCCION', SolUser: 'SOLUSER', SolPassword: 'sol-secret' }));
    expect(uploaded).toBe(certificate);
    expect(component.CertificatePassword).toBe('');
  });

  it('permite conservar credenciales y certificado existentes sin volver a subirlos', async () => {
    component.Mode = 'PRODUCCION';
    component.SolUser = 'SOLUSER';
    form.HasCertificate = form.HasCertificatePassword = form.HasSolPassword = true;
    await component.confirm();
    expect(service.configure).toHaveBeenCalled();
  });

  it('exige credenciales API cuando se habilitan guías', async () => {
    component.Mode = 'PRODUCCION';
    component.SolUser = 'SOLUSER';
    form.HasCertificate = form.HasCertificatePassword = form.HasSolPassword = true;
    component.GuideEnabled = true;
    await component.confirm();
    expect(service.configure).not.toHaveBeenCalled();
  });

  it('no avanza cuando el certificado es rechazado y permite corregirlo', async () => {
    service.configure.and.resolveTo({ ErrorStatus: true, Message: 'Certificado inválido' } as ResponseWsDto);
    spyOn(component.ConfigurationCompleted, 'emit');
    await component.confirm();
    expect(component.ConfigurationCompleted.emit).not.toHaveBeenCalled();
    expect(component.IsSaving).toBeFalse();
    expect(toastr.error).toHaveBeenCalledWith('Certificado inválido');
  });

  it('bloquea la confirmación si no se pudo cargar la configuración', async () => {
    service.findDataForm.and.resolveTo({ ErrorStatus: true, Message: 'Servicio no disponible' } as ResponseWsDto);
    await component.load();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('form')).toBeNull();
    await component.confirm();
    expect(service.configure).not.toHaveBeenCalled();
  });
});
