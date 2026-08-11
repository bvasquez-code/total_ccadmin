import { AuditTableEntity } from '../../../shared/model/entity/AuditTableEntity';

export class ClientAddressEntity extends AuditTableEntity {
  public ClientAddressID: number | null = null;
  public ClientCod: string = '';
  public Alias: string = '';
  public Names: string = '';
  public Phone: string = '';
  public Address: string = '';
  public Reference: string = '';
  public UbigeoCod: string = '';
  public Latitude: number | null = null;
  public Longitude: number | null = null;
  public Instructions: string = '';
  public IsDefault: string = 'N';
}
