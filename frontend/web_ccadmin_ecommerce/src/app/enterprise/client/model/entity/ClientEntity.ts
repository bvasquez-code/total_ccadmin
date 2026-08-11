import { AuditTableEntity } from '../../../shared/model/entity/AuditTableEntity';

export class ClientEntity extends AuditTableEntity {
  public ClientCod: string = '';
  public PersonCod: string = '';
  public Person: PersonEntity = new PersonEntity();
}

export class PersonEntity extends AuditTableEntity {
  public PersonCod: string = '';
  public PersonType: string = '';
  public DocumentType: string = '';
  public DocumentNum: string = '';
  public Names: string = '';
  public LastNames: string = '';
  public CommercialName: string = '';
  public BusinessName: string = '';
  public Address: string = '';
  public UbigeoCod: string = '';
  public Phone: string = '';
  public CellPhone: string = '';
  public Email: string = '';
}
