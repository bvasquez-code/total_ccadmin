import { CommonModule } from '@angular/common';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { DefaultUrlSerializer, Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { ResponseWsDto } from '../../../shared/model/dto/ResponseWsDto';
import { CounterfoilEntity } from '../../model/entity/CounterfoilEntity';
import { CounterfoilService } from '../../service/CounterfoilService';
import { CreatecounterfoilComponent } from './createcounterfoil.component';

describe('CreatecounterfoilComponent initialization', () => {
  let fixture: ComponentFixture<CreatecounterfoilComponent>;
  let component: CreatecounterfoilComponent;
  let counterfoilService: jasmine.SpyObj<CounterfoilService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    counterfoilService = jasmine.createSpyObj('CounterfoilService', ['formData', 'existsByDocumentTypeAndSeries', 'save']);
    router = jasmine.createSpyObj('Router', ['parseUrl', 'navigate'], { url: '/enterprise/system/pages/applicationinitialization' });
    router.parseUrl.and.callFake(url => new DefaultUrlSerializer().parse(url));
    const counterfoil = Object.assign(new CounterfoilEntity(), {
      CounterfoilCod: '01F001', DocumentType: '01', Series: 'F001', GroupDocument: 'F', Correlative: 0
    });
    const form = new ResponseWsDto();
    form.DataAdditional = [
      { Name: 'counterfoil', Data: counterfoil },
      { Name: 'documentType', Data: [{ Code: '01', Description: 'Factura' }] },
      { Name: 'store', Data: { StoreCod: 'T002' } }
    ];
    counterfoilService.formData.and.resolveTo(form);
    const available = new ResponseWsDto();
    available.Data = false;
    counterfoilService.existsByDocumentTypeAndSeries.and.resolveTo(available);
    counterfoilService.save.and.resolveTo(new ResponseWsDto());
    await TestBed.configureTestingModule({
      imports: [CommonModule, FormsModule],
      declarations: [CreatecounterfoilComponent],
      providers: [
        { provide: CounterfoilService, useValue: counterfoilService },
        { provide: Router, useValue: router },
        { provide: ToastrService, useValue: jasmine.createSpyObj('ToastrService', ['error', 'success']) }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(CreatecounterfoilComponent);
    component = fixture.componentInstance;
    component.InitializationMode = true;
    component.InitialCounterfoilCod = '01F001';
    component.InitialStoreCod = 'T001';
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  });

  it('permite editar la serie en el asistente y conserva el tipo de documento', () => {
    expect(fixture.nativeElement.querySelector('input[name="Series"]').readOnly).toBeFalse();
    expect(fixture.nativeElement.querySelector('select[name="DocumentType"]').disabled).toBeTrue();
    component.InitializationMode = false;
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('input[name="Series"]').readOnly).toBeTrue();
  });

  it('envía el cambio de serie y el último correlativo en un único guardado sin salir del asistente', async () => {
    const completed = spyOn(component.ConfigurationCompleted, 'emit');
    component.register.counterfoil.Series = 'F777';
    component.register.counterfoil.Correlative = 125;
    await component.save();
    expect(counterfoilService.save).toHaveBeenCalledOnceWith(component.register);
    expect(component.register.PreviousCounterfoilCod).toBe('01F001');
    expect(component.register.counterfoil.CounterfoilCod).toBe('01F777');
    expect(component.register.counterfoilStore.CounterfoilCod).toBe('01F777');
    expect(component.register.counterfoilStore.StoreCod).toBe('T001');
    expect(component.register.counterfoil.Correlative).toBe(125);
    expect(completed).toHaveBeenCalledOnceWith(component.register.counterfoil);
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('actualiza el correlativo de la serie actual sin solicitar reemplazo y admite cero', async () => {
    await component.save();
    expect(counterfoilService.save).toHaveBeenCalled();
    expect(component.register.PreviousCounterfoilCod).toBeUndefined();
    expect(component.register.counterfoil.Correlative).toBe(0);
    expect(counterfoilService.existsByDocumentTypeAndSeries).not.toHaveBeenCalled();
  });

  it('no guarda si falla la comprobación de disponibilidad de una nueva serie', async () => {
    const failure = new ResponseWsDto();
    failure.ErrorStatus = true;
    counterfoilService.existsByDocumentTypeAndSeries.and.resolveTo(failure);
    component.register.counterfoil.Series = 'F777';
    await component.save();
    expect(counterfoilService.save).not.toHaveBeenCalled();
    expect(component.IsSaving).toBeFalse();
  });

  it('mantiene el editor abierto si el backend rechaza el cambio', async () => {
    const failure = new ResponseWsDto();
    failure.ErrorStatus = true;
    counterfoilService.save.and.resolveTo(failure);
    const completed = spyOn(component.ConfigurationCompleted, 'emit');
    await component.save();
    expect(completed).not.toHaveBeenCalled();
    expect(component.IsSaving).toBeFalse();
  });
});
