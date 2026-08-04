import { PersonEntity } from '../entity/PersonEntity';

export interface SunatCompanyIdentityDto {
  ruc: string;
  legalName: string | null;
  taxpayerType: string | null;
  tradeName: string | null;
  registrationDate: string | null;
  businessStartDate: string | null;
  taxpayerStatus: string | null;
  taxpayerCondition: string | null;
  fiscalAddress: string | null;
  receiptIssuanceSystem: string | null;
  foreignTradeActivity: string | null;
  accountingSystem: string | null;
  economicActivities: string[];
  authorizedPaymentReceipts: string[];
  electronicIssuanceSystems: string[];
  electronicIssuerSince: string | null;
  electronicReceipts: string[];
  pleMemberSince: string | null;
  registries: string[];
  queryDate: string | null;
}

export interface SunatCompanyIdentityResponseDto {
  found: boolean;
  message: string;
  company: SunatCompanyIdentityDto | null;
}

export interface SunatRelatedTaxpayerDto {
  ruc: string | null;
  legalName: string | null;
  location: string | null;
  status: string | null;
}

export interface SunatPersonIdentityResponseDto {
  found: boolean;
  message: string;
  documentTypeCode: string;
  documentTypeName: string;
  documentNumber: string;
  resultCount: number;
  relatedTaxpayers: SunatRelatedTaxpayerDto[];
  queryDate: string | null;
}

export type PersonIdentityLookupSource = 'SUNAT' | 'INTERNAL';

export interface PersonIdentityLookupResultDto {
  person: PersonEntity | null;
  source: PersonIdentityLookupSource | null;
}
