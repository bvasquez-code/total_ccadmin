import { Injectable } from '@angular/core';
import { AppSetting } from '../../../config/app.setting';
import { PersonEntity } from '../../client/model/entity/ClientEntity';
import { ResponseWsDto } from '../../shared/model/dto/ResponseWsDto';
import { ApiService } from '../../shared/service/api.service';

@Injectable({ providedIn: 'root' })
export class BillingIdentityService {

  public constructor(private apiService: ApiService) {
  }

  public async findCompanyByRuc(ruc: string): Promise<PersonEntity | null> {
    const response: ResponseWsDto = await this.apiService.ExecuteGetService(
      `${AppSetting.API}/api/v1/delivery/billingIdentity/findCompanyByRuc`,
      { Ruc: ruc }
    );
    if (response.ErrorStatus || !response.Data) {
      return null;
    }
    const person = Object.assign(new PersonEntity(), response.Data);
    if (!person.BusinessName?.trim()) {
      person.BusinessName = person.CommercialName?.trim()
        || `${person.Names || ''} ${person.LastNames || ''}`.trim();
    }
    return person.BusinessName?.trim() ? person : null;
  }
}
