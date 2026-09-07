import { CommonModule } from '@angular/common';
import { ComponentFixture, TestBed, fakeAsync, flushMicrotasks, tick } from '@angular/core/testing';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { ClientEntity } from '../../model/entity/ClientEntity';
import { ClientService } from '../../service/client.service';
import { PersonEntity } from '../../../person/model/entity/PersonEntity';
import { PersonIdentityLookupResultDto } from '../../../person/model/dto/SunatIdentityDto';
import { PersonIdentityLookupService } from '../../../person/service/person-identity-lookup.service';
import { ResponseWsDto } from '../../../shared/model/dto/ResponseWsDto';
import { CreateclientComponent } from './createclient.component';
import { SpinnerService } from '../../../shared/service/spinner.service';

describe('CreateclientComponent identity lookup in sale modals', () => {
  let fixture: ComponentFixture<CreateclientComponent>;
  let component: CreateclientComponent;
  let clientService: jasmine.SpyObj<ClientService>;
  let personIdentityLookupService: jasmine.SpyObj<PersonIdentityLookupService>;
  let toastrService: jasmine.SpyObj<ToastrService>;
  let spinnerService: SpinnerService;

  beforeEach(async () => {
    clientService = jasmine.createSpyObj<ClientService>('ClientService', ['findByDocumentNum', 'Save']);
    personIdentityLookupService = jasmine.createSpyObj<PersonIdentityLookupService>(
      'PersonIdentityLookupService', ['findByDocument']
    );
    toastrService = jasmine.createSpyObj<ToastrService>('ToastrService', ['info', 'success', 'error']);
    clientService.findByDocumentNum.and.returnValue(Promise.resolve(responseWithData(null)));
    clientService.Save.and.callFake(async client => responseWithData(
      Object.assign(new ClientEntity(), client, { ClientCod: 'CLIENT-001' })
    ));
    personIdentityLookupService.findByDocument.and.returnValue(
      Promise.resolve({ person: null, source: null })
    );

    await TestBed.configureTestingModule({
      imports: [CommonModule],
      declarations: [CreateclientComponent],
      providers: [
        { provide: ClientService, useValue: clientService },
        { provide: PersonIdentityLookupService, useValue: personIdentityLookupService },
        { provide: ToastrService, useValue: toastrService },
        { provide: Router, useValue: { url: '/', parseUrl: () => ({ queryParams: {} }) } }
      ]
    }).compileComponents();
    spinnerService = TestBed.inject(SpinnerService);
  });

  it('automatically fills a new RUC and uses the existing save flow when confirmed', fakeAsync(() => {
    const company = companyIdentity();
    personIdentityLookupService.findByDocument.and.returnValue(
      Promise.resolve({ person: company, source: 'SUNAT' })
    );

    openRegistration(true);

    expect(personIdentityLookupService.findByDocument).toHaveBeenCalledOnceWith('06', '20123456789');
    expect(component.txtBusinessName?.nativeElement.value).toBe(company.BusinessName);
    expect(component.txtCommercialName?.nativeElement.value).toBe(company.CommercialName);
    expect(component.txtAddress?.nativeElement.value).toBe(company.Address);
    expect(component.IsDocumentLocked).toBeTrue();
    expect(clientService.Save).not.toHaveBeenCalled();

    const result = spyOn(component.ResultForm, 'emit');
    component.save();
    flushMicrotasks();

    expect(clientService.Save).toHaveBeenCalledTimes(1);
    expect(clientService.Save.calls.mostRecent().args[0].Person.DocumentType).toBe('06');
    expect(clientService.Save.calls.mostRecent().args[0].Person.BusinessName).toBe(company.BusinessName);
    expect(result).toHaveBeenCalledWith(jasmine.objectContaining({ ClientCod: 'CLIENT-001' }));
  }));

  it('keeps the searched RUC editable and accepts manual registration when no identity is found', fakeAsync(() => {
    openRegistration(true);

    expect(personIdentityLookupService.findByDocument).toHaveBeenCalledOnceWith('06', '20123456789');
    expect(component.txtDocumentNum.nativeElement.value).toBe('20123456789');
    expect(component.cboDocumentType.nativeElement.value).toBe('06');
    expect(component.IsLegalPerson).toBeTrue();
    expect(component.IsDocumentLocked).toBeFalse();
    expect(component.txtBusinessName?.nativeElement.readOnly).toBeFalse();
    expect(component.txtBusinessName?.nativeElement.value).toBe('');

    component.txtBusinessName!.nativeElement.value = 'EMPRESA MANUAL S.A.C.';
    component.txtCommercialName!.nativeElement.value = 'EMPRESA MANUAL';
    component.txtAddress!.nativeElement.value = 'AV. MANUAL 123';
    const result = spyOn(component.ResultForm, 'emit');
    component.save();
    flushMicrotasks();

    expect(clientService.Save).toHaveBeenCalledTimes(1);
    expect(clientService.Save.calls.mostRecent().args[0].Person.BusinessName).toBe('EMPRESA MANUAL S.A.C.');
    expect(result).toHaveBeenCalledWith(jasmine.objectContaining({ ClientCod: 'CLIENT-001' }));
  }));

  it('leaves manual entry available when the identity lookup fails', fakeAsync(() => {
    personIdentityLookupService.findByDocument.and.callFake(async () => {
      throw new Error('Identity service unavailable');
    });

    openRegistration(true);

    expect(component.IsSearchingIdentity).toBeFalse();
    expect(component.IsDocumentLocked).toBeFalse();
    expect(component.txtDocumentNum.nativeElement.readOnly).toBeFalse();
    expect(component.txtBusinessName?.nativeElement.readOnly).toBeFalse();
    expect(toastrService.error).toHaveBeenCalled();
    expect(clientService.Save).not.toHaveBeenCalled();
  }));

  it('does not repeat the lookup when billing already opened manual entry', fakeAsync(() => {
    openRegistration(false);

    expect(personIdentityLookupService.findByDocument).not.toHaveBeenCalled();
    expect(component.txtDocumentNum.nativeElement.value).toBe('20123456789');
    expect(component.cboDocumentType.nativeElement.value).toBe('06');
    expect(component.IsDocumentLocked).toBeFalse();
  }));

  it('prevents changing the document or saving while the automatic lookup is pending', fakeAsync(() => {
    let completeLookup!: (result: PersonIdentityLookupResultDto) => void;
    personIdentityLookupService.findByDocument.and.returnValue(new Promise(resolve => {
      completeLookup = resolve;
    }));

    openRegistration(true);

    expect(component.IsSearchingIdentity).toBeTrue();
    expect(spinnerService.isLoading).toBeTrue();
    expect(component.txtDocumentNum.nativeElement.readOnly).toBeTrue();
    expect(component.cboPersonType.nativeElement.disabled).toBeTrue();
    component.save();
    flushMicrotasks();
    expect(clientService.Save).not.toHaveBeenCalled();

    completeLookup({ person: null, source: null });
    flushMicrotasks();
    fixture.detectChanges();
    expect(component.IsSearchingIdentity).toBeFalse();
    expect(component.txtDocumentNum.nativeElement.readOnly).toBeFalse();
    expect(spinnerService.isLoading).toBeFalse();
  }));

  it('keeps the loader visible until the returned fields are populated', fakeAsync(() => {
    personIdentityLookupService.findByDocument.and.returnValue(
      Promise.resolve({ person: companyIdentity(), source: 'SUNAT' })
    );
    openRegistration(true, false);
    expect(spinnerService.isLoading).toBeTrue();
    expect(toastrService.success).not.toHaveBeenCalled();
    tick(100);
    fixture.detectChanges();
    expect(component.txtBusinessName?.nativeElement.value).toBe('EMPRESA NUEVA S.A.C.');
    expect(spinnerService.isLoading).toBeFalse();
    expect(toastrService.success).toHaveBeenCalled();
  }));

  it('fills an absent commercial name from the legal name and uses the same fallback when saving', fakeAsync(() => {
    const company = companyIdentity();
    company.CommercialName = '   ';
    personIdentityLookupService.findByDocument.and.returnValue(
      Promise.resolve({ person: company, source: 'SUNAT' })
    );
    openRegistration(true);
    expect(component.txtCommercialName?.nativeElement.value).toBe(company.BusinessName);
    component.txtCommercialName!.nativeElement.value = '';
    component.save();
    flushMicrotasks();
    expect(clientService.Save).toHaveBeenCalledTimes(1);
    expect(clientService.Save.calls.mostRecent().args[0].Person.CommercialName).toBe(company.BusinessName);
  }));

  function openRegistration(searchIdentity: boolean, waitForFields: boolean = true): void {
    fixture = TestBed.createComponent(CreateclientComponent);
    component = fixture.componentInstance;
    component.InvokeType = 'modal';
    component.InputDocumentType = '06';
    component.InputDocumentNum = '20123456789';
    component.SearchIdentityOnLoad = searchIdentity;
    fixture.detectChanges();
    tick(100, { processNewMacroTasksSynchronously: false });
    fixture.detectChanges();
    tick(0);
    fixture.detectChanges();
    if (waitForFields) {
      tick(100);
      fixture.detectChanges();
    }
  }

  function companyIdentity(): PersonEntity {
    return Object.assign(new PersonEntity(), {
      PersonType: '04', DocumentType: '06', DocumentNum: '20123456789',
      Names: '-', LastNames: '-', BusinessName: 'EMPRESA NUEVA S.A.C.',
      CommercialName: 'EMPRESA NUEVA', Address: 'AV. EJEMPLO 123'
    });
  }

  function responseWithData(data: unknown): ResponseWsDto {
    const response = new ResponseWsDto();
    response.Data = data;
    return response;
  }
});
