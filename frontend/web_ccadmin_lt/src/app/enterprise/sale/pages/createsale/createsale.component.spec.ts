import { ElementRef } from '@angular/core';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { ClientEntity } from '../../../client/model/entity/ClientEntity';
import { ClientService } from '../../../client/service/client.service';
import { PersonEntity } from '../../../person/model/entity/PersonEntity';
import { PersonIdentityLookupService } from '../../../person/service/person-identity-lookup.service';
import { ResponseWsDto } from '../../../shared/model/dto/ResponseWsDto';
import { SaleBillingEntity } from '../../model/entity/SaleBillingEntity';
import { SaleService } from '../../service/sale.service';
import { TicketSunatService } from '../../service/TicketSunatService';
import { CreatesaleComponent } from './createsale.component';

describe('CreatesaleComponent buyer billing', () => {
  let component: CreatesaleComponent;
  let saleService: jasmine.SpyObj<SaleService>;
  let personIdentityLookupService: jasmine.SpyObj<PersonIdentityLookupService>;
  let toastrService: jasmine.SpyObj<ToastrService>;
  let openClientModal: jasmine.Spy;
  let buyer: ClientEntity;
  let savedBilling: SaleBillingEntity;

  beforeEach(() => {
    saleService = jasmine.createSpyObj<SaleService>('SaleService', ['saveBilling', 'saveClientSale']);
    personIdentityLookupService = jasmine.createSpyObj<PersonIdentityLookupService>(
      'PersonIdentityLookupService', ['findByDocument']
    );
    toastrService = jasmine.createSpyObj<ToastrService>('ToastrService', ['info', 'success', 'error', 'warning']);
    const router = jasmine.createSpyObj<Router>('Router', ['parseUrl'], { url: '/' });
    router.parseUrl.and.returnValue({ queryParams: { SaleCod: 'SALE-001' } } as any);
    component = new CreatesaleComponent(
      saleService, router, jasmine.createSpyObj<TicketSunatService>('TicketSunatService', ['printSaleDocument']),
      toastrService, jasmine.createSpyObj<ClientService>('ClientService', ['findByDocumentNum']),
      personIdentityLookupService
    );
    component.SaleDetail.Headboard.SaleCod = 'SALE-001';
    component.SaleDetail.Headboard.NumTotalPrice = 1000;
    buyer = Object.assign(new ClientEntity(), {
      ClientCod: 'CLIENT-001', PersonCod: 'PERSON-001',
      Person: Object.assign(new PersonEntity(), {
        PersonCod: 'PERSON-001', PersonType: '04', DocumentType: '06', DocumentNum: '20123456789',
        BusinessName: 'EMPRESA COMPRADORA', CommercialName: 'EMPRESA COMPRADORA', Address: 'AV. EJEMPLO 123'
      })
    });
    component.SaleDetail.Headboard.ClientCod = buyer.ClientCod;
    component.SaleDetail.Headboard.Client = buyer;
    savedBilling = Object.assign(new SaleBillingEntity(), {
      SaleCod: 'SALE-001', PersonCod: 'PERSON-001', DocumentTypeRequest: '01',
      DocumentType: '06', DocumentNum: '20123456789', LegalName: 'EMPRESA COMPRADORA'
    });
    saleService.saveBilling.and.callFake(async () => responseWithData(savedBilling));
    saleService.saveClientSale.and.returnValue(Promise.resolve(responseWithData(buyer)));
    openClientModal = spyOn(component, 'OpenClientModal');
  });

  ['06', '6'].forEach(documentType => {
    it('uses the existing RUC buyer when selecting invoice: ' + documentType, async () => {
      buyer.Person.DocumentType = documentType;
      await component.selectDocumentType('01');

      expect(saleService.saveBilling).toHaveBeenCalledTimes(1);
      const request = saleService.saveBilling.calls.mostRecent().args[0];
      expect(request.SaleCod).toBe('SALE-001');
      expect(request.DocumentTypeRequest).toBe('01');
      expect(request.PersonCod).toBe(buyer.PersonCod);
      expect(request.Person).toBe(buyer.Person);
      expect(component.SaleDetail.SaleBilling).toBe(savedBilling);
      expect(component.enableButtonPay).toBeTrue();
      expect(openClientModal).not.toHaveBeenCalled();
      expect(personIdentityLookupService.findByDocument).not.toHaveBeenCalled();
    });
  });

  it('uses the client person code when the nested identity does not include it', async () => {
    buyer.Person.PersonCod = '';
    await component.selectDocumentType('01');
    expect(saleService.saveBilling.calls.mostRecent().args[0].PersonCod).toBe('PERSON-001');
  });

  it('keeps the existing receipt flow that resolves the DNI buyer on the server', async () => {
    buyer.Person.DocumentType = '01';
    buyer.Person.DocumentNum = '12345678';
    savedBilling.DocumentTypeRequest = '03';
    savedBilling.DocumentType = '01';
    savedBilling.DocumentNum = '12345678';
    savedBilling.LegalName = 'JUAN PEREZ';

    await component.selectDocumentType('03');

    const request = saleService.saveBilling.calls.mostRecent().args[0];
    expect(request.DocumentTypeRequest).toBe('03');
    expect(request.PersonCod).toBe('');
    expect(component.SaleDetail.SaleBilling).toBe(savedBilling);
    expect(component.enableButtonPay).toBeTrue();
    expect(openClientModal).not.toHaveBeenCalled();
    expect(personIdentityLookupService.findByDocument).not.toHaveBeenCalled();
  });

  it('replaces earlier billing with the RUC buyer when invoice is selected again', async () => {
    component.SaleDetail.SaleBilling = Object.assign(new SaleBillingEntity(), {
      DocumentTypeRequest: '01', DocumentType: '06', DocumentNum: '20987654321',
      PersonCod: 'OTHER-PERSON', LegalName: 'OTRA EMPRESA'
    });
    await component.selectDocumentType('01');
    expect(component.SaleDetail.SaleBilling?.PersonCod).toBe(buyer.PersonCod);
    expect(saleService.saveBilling.calls.mostRecent().args[0].Person).toBe(buyer.Person);
  });

  it('opens billing search for a DNI buyer instead of using that DNI for an invoice', async () => {
    buyer.Person.DocumentType = '01';
    buyer.Person.DocumentNum = '12345678';
    await component.selectDocumentType('01');
    expect(openClientModal).toHaveBeenCalledOnceWith('billing');
    expect(saleService.saveBilling).not.toHaveBeenCalled();
    expect(component.enableButtonPay).toBeFalse();
  });

  it('opens billing search for an incomplete RUC', async () => {
    buyer.Person.DocumentNum = '2012345';
    await component.selectDocumentType('01');
    expect(openClientModal).toHaveBeenCalledOnceWith('billing');
    expect(saleService.saveBilling).not.toHaveBeenCalled();
  });

  it('does not reuse client data unless the buyer is associated with the sale', async () => {
    component.SaleDetail.Headboard.ClientCod = '';
    await component.selectDocumentType('01');
    expect(openClientModal).toHaveBeenCalledOnceWith('billing');
    expect(saleService.saveBilling).not.toHaveBeenCalled();
  });

  it('synchronizes invoice billing after associating a RUC buyer', async () => {
    component.SelectedPaymentOption = '01';
    component.DocumentType = '01';
    component.SaleDetail.Headboard.ClientCod = '';
    const calls: string[] = [];
    saleService.saveClientSale.and.callFake(async () => {
      calls.push('client');
      return responseWithData(buyer);
    });
    saleService.saveBilling.and.callFake(async () => {
      calls.push('billing');
      return responseWithData(savedBilling);
    });

    await component.SaveClientSale(buyer);

    expect(calls).toEqual(['client', 'billing']);
    expect(saleService.saveBilling.calls.mostRecent().args[0].Person).toBe(buyer.Person);
    expect(component.SaleDetail.SaleBilling).toBe(savedBilling);
    expect(component.ShowClient).toBeTrue();
    expect(component.enableButtonPay).toBeTrue();
  });

  it('preserves the requested billing of web sales', async () => {
    component.SaleDetail.SaleChannel.ChannelCod = 'WEB';
    savedBilling.PersonCod = 'WEB-PERSON';
    component.SaleDetail.SaleBilling = savedBilling;
    await component.selectDocumentType('01');
    expect(saleService.saveBilling).not.toHaveBeenCalled();
    expect(component.SaleDetail.SaleBilling.PersonCod).toBe('WEB-PERSON');
  });

  it('does not change billing when associating the person for an advance', async () => {
    component.ClientSearchMode = 'advance';
    component.SelectedPaymentOption = 'advance';
    component.DocumentType = '03';
    await component.SaveClientSale(buyer);
    expect(saleService.saveBilling).not.toHaveBeenCalled();
  });

  it('reports billing save errors and prevents payment without replacing persisted billing locally', async () => {
    const failedResponse = new ResponseWsDto();
    failedResponse.ErrorStatus = true;
    failedResponse.Message = 'No se pudo guardar Billing';
    saleService.saveBilling.and.returnValue(Promise.resolve(failedResponse));
    component.enableButtonPay = true;

    await component.selectDocumentType('01');

    expect(component.SaleDetail.SaleBilling).toBeNull();
    expect(component.enableButtonPay).toBeFalse();
    expect(toastrService.error).toHaveBeenCalledWith(failedResponse.Message);
    expect(openClientModal).not.toHaveBeenCalled();
  });

  it('continues using the same save flow for a separately searched billing person', async () => {
    component.ClientSearchMode = 'billing';
    component.DocumentType = '01';
    component.cboDocumentType = new ElementRef({ value: '06' } as HTMLSelectElement);
    component.txtDocumentNum = new ElementRef({ value: '20123456789' } as HTMLInputElement);
    personIdentityLookupService.findByDocument.and.returnValue(
      Promise.resolve({ person: buyer.Person, source: 'SUNAT' })
    );
    await component.findByDocumentNum();
    expect(saleService.saveBilling.calls.mostRecent().args[0].Person).toBe(buyer.Person);
    expect(component.SaleDetail.SaleBilling).toBe(savedBilling);
    expect(component.ShowClient).toBeTrue();
  });

  function responseWithData(data: unknown): ResponseWsDto {
    const response = new ResponseWsDto();
    response.Data = data;
    return response;
  }
});
