import { Injectable } from '@angular/core';
import { AppSetting } from 'src/app/config/app.setting';
import { ApiService } from '../../compartido/service/api.service';
import { ResponseWsDto } from '../../shared/model/dto/ResponseWsDto';
import {
  PersonIdentityLookupResultDto,
  SunatCompanyIdentityDto,
  SunatCompanyIdentityResponseDto,
  SunatPersonIdentityResponseDto,
  SunatRelatedTaxpayerDto
} from '../model/dto/SunatIdentityDto';
import { PersonEntity } from '../model/entity/PersonEntity';
import { PersonService } from './person.service';

@Injectable({
  providedIn: 'root'
})
export class PersonIdentityLookupService {

  public constructor(
    private apiService: ApiService,
    private personService: PersonService
  ) {
  }

  async findByDocument(
    documentType: string,
    documentNumber: string
  ): Promise<PersonIdentityLookupResultDto> {
    const internalPerson = await this.findInternallySafely(documentType, documentNumber);
    const sunatPerson = await this.findInSunatSafely(documentType, documentNumber);

    if (internalPerson && sunatPerson) {
      return {
        person: this.mergeIdentityData(internalPerson, sunatPerson),
        source: 'SUNAT'
      };
    }

    if (sunatPerson) {
      return { person: sunatPerson, source: 'SUNAT' };
    }

    if (internalPerson) {
      return { person: internalPerson, source: 'INTERNAL' };
    }

    return { person: null, source: null };
  }

  private async findInternallySafely(
    documentType: string,
    documentNumber: string
  ): Promise<PersonEntity | null> {
    try {
      return await this.findInternally(documentType, documentNumber);
    } catch {
      return null;
    }
  }

  private async findInSunatSafely(
    documentType: string,
    documentNumber: string
  ): Promise<PersonEntity | null> {
    try {
      return await this.findInSunat(documentType, documentNumber);
    } catch {
      return null;
    }
  }

  private async findInSunat(
    documentType: string,
    documentNumber: string
  ): Promise<PersonEntity | null> {
    if (documentType === '06') {
      return this.findCompanyByRuc(documentNumber);
    }
    return this.findNaturalPersonByDocument(documentType, documentNumber);
  }

  private async findCompanyByRuc(documentNumber: string): Promise<PersonEntity | null> {
    const url = `${AppSetting.API_SUNAT_IDENTITY}/api/v1/sunatIdentity/findCompanyByRuc`;
    const response: ResponseWsDto = await this.apiService.ExecuteGetService(url, {
      Ruc: documentNumber
    });
    const data = response.Data as SunatCompanyIdentityResponseDto | null;

    if (response.ErrorStatus || !data?.found || !data.company?.legalName) {
      return null;
    }

    return this.mapCompany(data.company, documentNumber);
  }

  private async findNaturalPersonByDocument(
    documentType: string,
    documentNumber: string
  ): Promise<PersonEntity | null> {
    const url = `${AppSetting.API_SUNAT_IDENTITY}/api/v1/sunatIdentity/findPersonByDocument`;
    const response: ResponseWsDto = await this.apiService.ExecuteGetService(url, {
      DocumentType: documentType,
      DocumentNumber: documentNumber
    });
    const data = response.Data as SunatPersonIdentityResponseDto | null;

    if (response.ErrorStatus || !data?.found || !data.relatedTaxpayers?.length) {
      return null;
    }

    const relatedTaxpayer = this.selectNaturalPersonTaxpayer(data.relatedTaxpayers);
    return relatedTaxpayer ? this.mapNaturalPerson(relatedTaxpayer, documentType, documentNumber) : null;
  }

  private async findInternally(
    documentType: string,
    documentNumber: string
  ): Promise<PersonEntity | null> {
    const response: ResponseWsDto = await this.personService.findByDocumentNum(
      documentType,
      documentNumber
    );

    if (response.ErrorStatus || !response.Data) {
      return null;
    }

    return Object.assign(new PersonEntity(), response.Data);
  }

  private mergeIdentityData(
    internalPerson: PersonEntity,
    identityPerson: PersonEntity
  ): PersonEntity {
    const mergedPerson = Object.assign(new PersonEntity(), internalPerson);

    mergedPerson.PersonType = this.preferIdentityValue(
      identityPerson.PersonType,
      mergedPerson.PersonType
    );
    mergedPerson.DocumentType = this.preferIdentityValue(
      identityPerson.DocumentType,
      mergedPerson.DocumentType
    );
    mergedPerson.DocumentNum = this.preferIdentityValue(
      identityPerson.DocumentNum,
      mergedPerson.DocumentNum
    );
    mergedPerson.Names = this.preferIdentityValue(
      identityPerson.Names,
      mergedPerson.Names
    );
    mergedPerson.LastNames = this.preferIdentityValue(
      identityPerson.LastNames,
      mergedPerson.LastNames
    );
    mergedPerson.CommercialName = this.preferIdentityValue(
      identityPerson.CommercialName,
      mergedPerson.CommercialName
    );
    mergedPerson.BusinessName = this.preferIdentityValue(
      identityPerson.BusinessName,
      mergedPerson.BusinessName
    );
    mergedPerson.Address = this.preferIdentityValue(
      identityPerson.Address,
      mergedPerson.Address
    );

    return mergedPerson;
  }

  private preferIdentityValue(identityValue: string, internalValue: string): string {
    return this.meaningfulValue(identityValue) ? identityValue.trim() : internalValue;
  }

  private mapCompany(company: SunatCompanyIdentityDto, documentNumber: string): PersonEntity {
    const person = new PersonEntity();
    person.PersonType = '04';
    person.DocumentType = '06';
    person.DocumentNum = documentNumber;
    person.Names = '-';
    person.LastNames = '-';
    person.BusinessName = company.legalName?.trim() || '';
    person.CommercialName = this.meaningfulValue(company.tradeName)
      ? company.tradeName!.trim()
      : '';
    person.Address = this.meaningfulValue(company.fiscalAddress)
      ? company.fiscalAddress!.trim()
      : '';
    return person;
  }

  private mapNaturalPerson(
    relatedTaxpayer: SunatRelatedTaxpayerDto,
    documentType: string,
    documentNumber: string
  ): PersonEntity | null {
    const nameParts = this.splitNaturalPersonName(relatedTaxpayer.legalName);
    if (!nameParts) {
      return null;
    }

    const person = new PersonEntity();
    person.PersonType = '01';
    person.DocumentType = documentType;
    person.DocumentNum = documentNumber;
    person.Names = nameParts.names;
    person.LastNames = nameParts.lastNames;
    return person;
  }

  private selectNaturalPersonTaxpayer(
    relatedTaxpayers: SunatRelatedTaxpayerDto[]
  ): SunatRelatedTaxpayerDto | null {
    const naturalTaxpayer = relatedTaxpayers.find(taxpayer => taxpayer.ruc?.startsWith('10'));
    if (naturalTaxpayer) {
      return naturalTaxpayer;
    }

    return relatedTaxpayers.find(taxpayer =>
      !taxpayer.ruc && !!taxpayer.legalName?.trim()
    ) || null;
  }

  private splitNaturalPersonName(
    legalName: string | null
  ): { names: string; lastNames: string } | null {
    if (!legalName) {
      return null;
    }

    const words = legalName
      .replace(/[^a-zA-Z\u00C0-\u017F\s]/g, ' ')
      .replace(/\s+/g, ' ')
      .trim()
      .split(' ')
      .filter(word => !!word);

    if (words.length < 2) {
      return null;
    }

    const lastNameWordCount = words.length >= 3 ? 2 : 1;
    return {
      lastNames: words.slice(0, lastNameWordCount).join(' '),
      names: words.slice(lastNameWordCount).join(' ')
    };
  }

  private meaningfulValue(value: string | null): boolean {
    return !!value && value.trim() !== '-';
  }
}
