export interface SunatCompanyIdentityDto {
  ruc: string;
  legalName: string | null;
  tradeName: string | null;
  fiscalAddress: string | null;
}

export interface SunatCompanyIdentityResponseDto {
  found: boolean;
  message: string;
  company: SunatCompanyIdentityDto | null;
}
