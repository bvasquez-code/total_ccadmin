import { ResponseWsDto } from '../../shared/model/dto/ResponseWsDto';
import { ApiService } from '../../compartido/service/api.service';
import { PersonEntity } from '../model/entity/PersonEntity';
import { PersonIdentityLookupService } from './person-identity-lookup.service';
import { PersonService } from './person.service';

describe('PersonIdentityLookupService', () => {
  let apiService: jasmine.SpyObj<ApiService>;
  let personService: jasmine.SpyObj<PersonService>;
  let service: PersonIdentityLookupService;

  beforeEach(() => {
    apiService = jasmine.createSpyObj<ApiService>('ApiService', ['ExecuteGetService']);
    personService = jasmine.createSpyObj<PersonService>('PersonService', ['findByDocumentNum']);
    personService.findByDocumentNum.and.returnValue(Promise.resolve(responseWithData(null)));
    service = new PersonIdentityLookupService(apiService, personService);
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

  function responseWithData(data: unknown): ResponseWsDto {
    const response = new ResponseWsDto();
    response.Status = '200';
    response.Data = data;
    return response;
  }
});
