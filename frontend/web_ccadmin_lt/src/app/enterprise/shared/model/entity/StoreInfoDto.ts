import { StoreEntity } from "./StoreEntity";
import { CompanyEntity } from "./CompanyEntity";

export class StoreInfoDto {
  public Store: StoreEntity;
  public StoreUbigeo: string;
  public Company: CompanyEntity;
  public CompanyUbigeo: string;

  public constructor() {
    this.Store = new StoreEntity();
    this.StoreUbigeo = '';
    this.Company = new CompanyEntity();
    this.CompanyUbigeo = '';
  }
}