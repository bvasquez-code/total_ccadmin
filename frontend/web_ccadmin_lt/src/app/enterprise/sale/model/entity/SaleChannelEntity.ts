import { AuditTableEntity } from 'src/app/enterprise/shared/model/entity/AuditTableEntity';
import { SaleConstants } from '../constants/SaleConstants';

export class SaleChannelEntity extends AuditTableEntity {
  public SaleCod: string = '';
  public ChannelCod: string = SaleConstants.COMMERCIAL_CHANNEL_IN_PERSON;

  constructor() {
    super();
  }
}
