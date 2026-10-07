export type SunatInitializationMode = 'NONE' | 'BETA' | 'PRODUCCION';

export interface SunatInitializationForm {
  Mode: SunatInitializationMode;
  IssuerRuc: string;
  SolUser: string;
  HasSolPassword: boolean;
  HasCertificate: boolean;
  HasCertificatePassword: boolean;
  GuideEnabled: boolean;
  GuideClientId: string;
  HasGuideClientSecret: boolean;
  BetaEndpoint: string;
  ProductionEndpoint: string;
  GuideEndpoint: string;
  GuideTokenEndpoint: string;
}
