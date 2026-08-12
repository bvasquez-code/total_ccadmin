import { Injectable } from '@angular/core';
import { AppSetting } from '../../../config/app.setting';
import { PersonEntity } from '../../client/model/entity/ClientEntity';
import { ResponseWsDto } from '../../shared/model/dto/ResponseWsDto';
import { ApiService } from '../../shared/service/api.service';
import {
  SunatCompanyIdentityDto,
  SunatCompanyIdentityResponseDto
} from '../model/dto/SunatCompanyIdentityDto';

@Injectable({ providedIn: 'root' })
export class BillingIdentityService {

  public constructor(private apiService: ApiService) {
  }

  public async findCompanyByRuc(ruc: string): Promise<PersonEntity | null> {
    const response: ResponseWsDto = await this.apiService.ExecuteGetService(
      `${AppSetting.API_SUNAT_IDENTITY}/api/v1/sunatIdentity/findCompanyByRuc`,
      { Ruc: ruc }
    );
    const data = response.Data as SunatCompanyIdentityResponseDto | null;
    if (response.ErrorStatus || !data?.found || !data.company?.legalName) {
      return null;
    }
    return this.mapCompany(data.company, ruc);
  }

  private mapCompany(company: SunatCompanyIdentityDto, ruc: string): PersonEntity {
    const person = new PersonEntity();
    person.PersonType = '04';
    person.DocumentType = '06';
    person.DocumentNum = ruc;
    person.Names = '-';
    person.LastNames = '-';
    person.BusinessName = company.legalName?.trim() || '';
    person.CommercialName = company.tradeName?.trim() || '';
    person.Address = company.fiscalAddress?.trim() || '';
    return person;
  }
}
