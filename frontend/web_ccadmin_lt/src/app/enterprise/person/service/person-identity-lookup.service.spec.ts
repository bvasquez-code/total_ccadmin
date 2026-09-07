import { ResponseWsDto } from '../../shared/model/dto/ResponseWsDto';
import { ApiService } from '../../compartido/service/api.service';
import { PersonEntity } from '../model/entity/PersonEntity';
import { PersonIdentityLookupService } from './person-identity-lookup.service';
import { PersonService } from './person.service';
import { SpinnerService } from '../../shared/service/spinner.service';

describe('PersonIdentityLookupService', () => {
  let apiService: jasmine.SpyObj<ApiService>;
  let personService: jasmine.SpyObj<PersonService>;
  let service: PersonIdentityLookupService;
  let spinnerService: SpinnerService;

  beforeEach(() => {
    apiService = jasmine.createSpyObj<ApiService>('ApiService', ['ExecuteGetService']);
    personService = jasmine.createSpyObj<PersonService>('PersonService', ['findByDocumentNum']);
    personService.findByDocumentNum.and.returnValue(Promise.resolve(responseWithData(null)));
    spinnerService = new SpinnerService();
    service = new PersonIdentityLookupService(apiService, personService, spinnerService);
  });

  it('maps a company returned by SUNAT', async () => {
    apiService.ExecuteGetService.and.returnValue(Promise.resolve(responseWithData({
      found: true,
      message: 'OK',
      company: {
        ruc: '20123456789',
        legalName: 'EMPRESA EJEMPLO S.A.C.',
        tradeName: 'EMPRESA EJEMPLO',
        fiscalAddress: 'AV. EJEMPLO 123'
      }
    })));

    const result = await service.findByDocument('06', '20123456789');

    expect(result.source).toBe('SUNAT');
    expect(result.person?.PersonType).toBe('04');
    expect(result.person?.BusinessName).toBe('EMPRESA EJEMPLO S.A.C.');
    expect(result.person?.CommercialName).toBe('EMPRESA EJEMPLO');
    expect(result.person?.Address).toBe('AV. EJEMPLO 123');
    expect(personService.findByDocumentNum).toHaveBeenCalledOnceWith('06', '20123456789');
  });

  it('uses the internal system when SUNAT has no usable response', async () => {
    const externalResponse = new ResponseWsDto();
    externalResponse.ErrorStatus = true;
    apiService.ExecuteGetService.and.returnValue(Promise.resolve(externalResponse));

    const internalPerson = new PersonEntity();
    internalPerson.DocumentType = '01';
    internalPerson.DocumentNum = '12345678';
    internalPerson.Names = 'JUAN';
    internalPerson.LastNames = 'PEREZ';
    personService.findByDocumentNum.and.returnValue(
      Promise.resolve(responseWithData(internalPerson))
    );

    const result = await service.findByDocument('01', '12345678');

    expect(result.source).toBe('INTERNAL');
    expect(result.person?.Names).toBe('JUAN');
    expect(personService.findByDocumentNum).toHaveBeenCalledOnceWith('01', '12345678');
    expect(apiService.ExecuteGetService).toHaveBeenCalledTimes(1);
  });

  it('selects the personal RUC and separates the SUNAT legal name', async () => {
    apiService.ExecuteGetService.and.returnValue(Promise.resolve(responseWithData({
      found: true,
      message: 'OK',
      documentTypeCode: '01',
      documentTypeName: 'DNI',
      documentNumber: '12345678',
      resultCount: 2,
      relatedTaxpayers: [
        {
          ruc: '20123456789',
          legalName: 'EMPRESA RELACIONADA S.A.C.',
          location: 'LIMA',
          status: 'ACTIVO'
        },
        {
          ruc: '10123456786',
          legalName: 'PEREZ GOMEZ JUAN CARLOS',
          location: 'LIMA',
          status: 'ACTIVO'
        }
      ],
      queryDate: null
    })));

    const result = await service.findByDocument('01', '12345678');

    expect(result.source).toBe('SUNAT');
    expect(result.person?.LastNames).toBe('PEREZ GOMEZ');
    expect(result.person?.Names).toBe('JUAN CARLOS');
    expect(personService.findByDocumentNum).toHaveBeenCalledOnceWith('01', '12345678');
  });

  it('maps a DNI fallback response without requiring a RUC', async () => {
    apiService.ExecuteGetService.and.returnValue(Promise.resolve(responseWithData({
      found: true,
      message: 'OK',
      documentTypeCode: '01',
      documentTypeName: 'DNI',
      documentNumber: '77975840',
      resultCount: 1,
      relatedTaxpayers: [
        {
          ruc: null,
          legalName: 'MEL ROBLES KATHERIN MARGOTH',
          location: null,
          status: null
        }
      ],
      queryDate: null
    })));

    const result = await service.findByDocument('01', '77975840');

    expect(result.source).toBe('SUNAT');
    expect(result.person?.LastNames).toBe('MEL ROBLES');
    expect(result.person?.Names).toBe('KATHERIN MARGOTH');
    expect(apiService.ExecuteGetService).toHaveBeenCalledTimes(1);
    expect(personService.findByDocumentNum).toHaveBeenCalledOnceWith('01', '77975840');
  });

  it('uses the internal system once when the identity request fails', async () => {
    apiService.ExecuteGetService.and.returnValue(Promise.reject(new Error('Connection refused')));

    const internalPerson = new PersonEntity();
    internalPerson.DocumentType = '01';
    internalPerson.DocumentNum = '12345678';
    internalPerson.Names = 'JUAN';
    internalPerson.LastNames = 'PEREZ';
    personService.findByDocumentNum.and.returnValue(
      Promise.resolve(responseWithData(internalPerson))
    );

    const result = await service.findByDocument('01', '12345678');

    expect(result.source).toBe('INTERNAL');
    expect(apiService.ExecuteGetService).toHaveBeenCalledTimes(1);
    expect(personService.findByDocumentNum).toHaveBeenCalledOnceWith('01', '12345678');
  });

  it('queries internally first and preserves contact data when identity updates names', async () => {
    const requestOrder: string[] = [];
    const internalPerson = new PersonEntity();
    internalPerson.PersonCod = 'PERSON-001';
    internalPerson.PersonType = '01';
    internalPerson.DocumentType = '01';
    internalPerson.DocumentNum = '77975840';
    internalPerson.Names = 'NOMBRE ANTERIOR';
    internalPerson.LastNames = 'APELLIDO ANTERIOR';
    internalPerson.Address = 'DIRECCION INTERNA';
    internalPerson.UbigeoCod = '150101';
    internalPerson.CellPhone = '999888777';
    internalPerson.Phone = '014445555';
    internalPerson.Email = 'persona@correo.pe';

    personService.findByDocumentNum.and.callFake(async () => {
      requestOrder.push('INTERNAL');
      return responseWithData(internalPerson);
    });
    apiService.ExecuteGetService.and.callFake(async () => {
      requestOrder.push('IDENTITY');
      return responseWithData({
        found: true,
        message: 'OK',
        documentTypeCode: '01',
        documentTypeName: 'DNI',
        documentNumber: '77975840',
        resultCount: 1,
        relatedTaxpayers: [
          {
            ruc: null,
            legalName: 'MEL ROBLES KATHERIN MARGOTH',
            location: null,
            status: null
          }
        ],
        queryDate: null
      });
    });

    const result = await service.findByDocument('01', '77975840');

    expect(requestOrder).toEqual(['INTERNAL', 'IDENTITY']);
    expect(result.source).toBe('SUNAT');
    expect(result.person?.PersonCod).toBe('PERSON-001');
    expect(result.person?.LastNames).toBe('MEL ROBLES');
    expect(result.person?.Names).toBe('KATHERIN MARGOTH');
    expect(result.person?.Address).toBe('DIRECCION INTERNA');
    expect(result.person?.UbigeoCod).toBe('150101');
    expect(result.person?.CellPhone).toBe('999888777');
    expect(result.person?.Phone).toBe('014445555');
    expect(result.person?.Email).toBe('persona@correo.pe');
  });

  it('does not replace company fields when identity returns empty values', async () => {
    const internalPerson = new PersonEntity();
    internalPerson.PersonCod = 'PERSON-002';
    internalPerson.PersonType = '04';
    internalPerson.DocumentType = '06';
    internalPerson.DocumentNum = '20123456789';
    internalPerson.BusinessName = 'RAZON SOCIAL ANTERIOR';
    internalPerson.CommercialName = 'NOMBRE COMERCIAL INTERNO';
    internalPerson.Address = 'AV. INTERNA 456';
    internalPerson.CellPhone = '999111222';
    internalPerson.Email = 'empresa@correo.pe';
    personService.findByDocumentNum.and.returnValue(
      Promise.resolve(responseWithData(internalPerson))
    );
    apiService.ExecuteGetService.and.returnValue(Promise.resolve(responseWithData({
      found: true,
      message: 'OK',
      company: {
        ruc: '20123456789',
        legalName: 'RAZON SOCIAL ACTUALIZADA S.A.C.',
        tradeName: '-',
        fiscalAddress: null
      }
    })));

    const result = await service.findByDocument('06', '20123456789');

    expect(result.person?.PersonCod).toBe('PERSON-002');
    expect(result.person?.BusinessName).toBe('RAZON SOCIAL ACTUALIZADA S.A.C.');
    expect(result.person?.CommercialName).toBe('NOMBRE COMERCIAL INTERNO');
    expect(result.person?.Address).toBe('AV. INTERNA 456');
    expect(result.person?.CellPhone).toBe('999111222');
    expect(result.person?.Email).toBe('empresa@correo.pe');
  });

  [null, '', '   ', '-'].forEach(tradeName => {
    it('uses the legal name for a missing trade name: ' + JSON.stringify(tradeName), async () => {
      apiService.ExecuteGetService.and.returnValue(Promise.resolve(responseWithData({
        found: true,
        company: { legalName: 'EMPRESA SIN NOMBRE COMERCIAL', tradeName }
      })));
      const result = await service.findByDocument('06', '20123456789');
      expect(result.person?.CommercialName).toBe('EMPRESA SIN NOMBRE COMERCIAL');
      expect(spinnerService.isLoading).toBeFalse();
    });
  });

  it('uses the internal legal name when identity fails and the stored commercial name is empty', async () => {
    personService.findByDocumentNum.and.returnValue(Promise.resolve(responseWithData(
      Object.assign(new PersonEntity(), {
        PersonType: '04', DocumentType: '06', DocumentNum: '20123456789',
        BusinessName: 'EMPRESA INTERNA', CommercialName: ''
      })
    )));
    apiService.ExecuteGetService.and.callFake(async () => { throw new Error('Unavailable'); });
    const result = await service.findByDocument('06', '20123456789');
    expect(result.person?.CommercialName).toBe('EMPRESA INTERNA');
    expect(spinnerService.isLoading).toBeFalse();
  });

  it('keeps the loader throughout identity lookup and releases it when no data can be obtained', async () => {
    let lookupStarted!: () => void;
    let finishLookup!: () => void;
    const started = new Promise<void>(resolve => lookupStarted = resolve);
    const finished = new Promise<void>(resolve => finishLookup = resolve);
    apiService.ExecuteGetService.and.callFake(async () => {
      lookupStarted();
      await finished;
      throw new Error('Identity unavailable');
    });
    const result = service.findByDocument('06', '20123456789');
    expect(spinnerService.isLoading).toBeTrue();
    await started;
    expect(spinnerService.isLoading).toBeTrue();
    finishLookup();
    expect((await result).person).toBeNull();
    expect(spinnerService.isLoading).toBeFalse();
  });

  function responseWithData(data: unknown): ResponseWsDto {
    const response = new ResponseWsDto();
    response.Status = '200';
    response.Data = data;
    return response;
  }
});
