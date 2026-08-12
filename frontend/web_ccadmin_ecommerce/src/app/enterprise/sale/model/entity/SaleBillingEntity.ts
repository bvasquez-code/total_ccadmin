import { AuditTableEntity } from '../../../shared/model/entity/AuditTableEntity';
import { PersonEntity } from '../../../client/model/entity/ClientEntity';

export class SaleBillingEntity extends AuditTableEntity {
  public SaleCod: string = '';
  public PersonCod: string = '';
  public DocumentTypeRequest: string = '03';
  public DocumentType: string = '';
  public DocumentNum: string = '';
  public LegalName: string = '';
  public CommercialName: string = '';
  public Address: string = '';
  public UbigeoCod: string = '';
  public Person: PersonEntity = new PersonEntity();
}
