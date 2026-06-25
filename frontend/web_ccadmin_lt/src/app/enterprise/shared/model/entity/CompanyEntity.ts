import { AuditTableEntity } from "./AuditTableEntity";

export class CompanyEntity extends AuditTableEntity {
  public CompanyCod: string = '';
  public TaxId: string = '';
  public LegalName: string = '';
  public TradeName: string = '';
  public FiscalAddress: string = '';
  public Address: string = '';
  public UbigeoCod: string = '';
  public CountryCode: string = 'PE';
  public Phone: string = '';
  public Email: string = '';
  public Website: string = '';
  public LogoPath: string = '';

  public constructor() {
    super();
  }
}