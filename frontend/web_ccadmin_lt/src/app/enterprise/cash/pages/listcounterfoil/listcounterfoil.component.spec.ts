import { CommonModule } from '@angular/common';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ToastrService } from 'ngx-toastr';
import { DataSesionService } from '../../../compartido/service/datasesion.service';
import { ResponsePageSearch } from '../../../shared/model/dto/ResponsePageSearch';
import { ResponseWsDto } from '../../../shared/model/dto/ResponseWsDto';
import { TableComponent } from '../../../shared/component/table/table.component';
import { CounterfoilEntity } from '../../model/entity/CounterfoilEntity';
import { CounterfoilService } from '../../service/CounterfoilService';
import { ListcounterfoilComponent } from './listcounterfoil.component';

describe('ListcounterfoilComponent initialization', () => {
  let fixture: ComponentFixture<ListcounterfoilComponent>;
  let component: ListcounterfoilComponent;
  let counterfoilService: jasmine.SpyObj<CounterfoilService>;

  beforeEach(async () => {
    counterfoilService = jasmine.createSpyObj('CounterfoilService', ['formData', 'findAll', 'save', 'enable', 'disable']);
    const counterfoil = Object.assign(new CounterfoilEntity(), {
      CounterfoilCod: '01F001', DocumentType: '01', Series: 'F001', GroupDocument: 'F', Correlative: 0
    });
    const form = new ResponseWsDto();
    form.DataAdditional = [{ Name: 'documentType', Data: [{ Code: '01', Description: 'Factura' }] }];
    counterfoilService.formData.and.resolveTo(form);
    const page = new ResponsePageSearch<CounterfoilEntity>();
    page.resultSearch = [counterfoil];
    page.TotalPages = 1;
    page.TotalResult = 1;
    page.Page = 1;
    const response = new ResponseWsDto();
    response.Data = page;
    counterfoilService.findAll.and.resolveTo(response);
    await TestBed.configureTestingModule({
      imports: [CommonModule],
      declarations: [ListcounterfoilComponent, TableComponent],
      providers: [
        { provide: CounterfoilService, useValue: counterfoilService },
        { provide: DataSesionService, useValue: { getSessionStorageDto: () => ({ StoreCod: 'T002' }) } },
        { provide: ToastrService, useValue: jasmine.createSpyObj('ToastrService', ['error', 'success']) }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
    fixture = TestBed.createComponent(ListcounterfoilComponent);
    component = fixture.componentInstance;
    component.InitializationMode = true;
    component.InitialStoreCod = 'T001';
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  });

  it('carga los talonarios de la tienda configurada y permite conservarlos sin escribir datos', () => {
    expect(counterfoilService.findAll).toHaveBeenCalledOnceWith('', 1, 'T001');
    expect(fixture.nativeElement.textContent).toContain('F001');
    const completed = spyOn(component.ConfigurationCompleted, 'emit');
    const button = Array.from(fixture.nativeElement.querySelectorAll('button'))
      .find((element: any) => element.textContent.includes('Conservar talonarios')) as HTMLButtonElement;
    button.click();
    expect(completed).toHaveBeenCalled();
    expect(counterfoilService.save).not.toHaveBeenCalled();
    expect(counterfoilService.disable).not.toHaveBeenCalled();
  });

  it('abre el editor dentro del paso usando la acción de la tabla compartida', () => {
    fixture.nativeElement.querySelector('button[aria-label="Editar talonario"]').click();
    fixture.detectChanges();
    expect(component.IsEditing).toBeTrue();
    expect(component.SelectedCounterfoilCod).toBe('01F001');
    expect(fixture.nativeElement.querySelector('app-createcounterfoil')).not.toBeNull();
  });

  it('actualiza la lista después de guardar sin avanzar automáticamente al producto', async () => {
    const completed = spyOn(component.ConfigurationCompleted, 'emit');
    component.editCounterfoil('01F001');
    component.counterfoilSaved();
    await fixture.whenStable();
    expect(component.IsEditing).toBeFalse();
    expect(counterfoilService.findAll).toHaveBeenCalledTimes(2);
    expect(completed).not.toHaveBeenCalled();
  });

  it('impide continuar si no se pudieron cargar los talonarios', async () => {
    const failure = new ResponseWsDto();
    failure.ErrorStatus = true;
    counterfoilService.findAll.and.resolveTo(failure);
    await component.findAll(1, '');
    fixture.detectChanges();
    const button = Array.from(fixture.nativeElement.querySelectorAll('button'))
      .find((element: any) => element.textContent.includes('Conservar talonarios')) as HTMLButtonElement;
    expect(button.disabled).toBeTrue();
  });
});
