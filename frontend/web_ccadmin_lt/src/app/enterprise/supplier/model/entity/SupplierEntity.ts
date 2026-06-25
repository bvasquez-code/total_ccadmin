import { AuditTableEntity } from '../../../shared/model/entity/AuditTableEntity';
import { PersonEntity } from '../../../person/model/entity/PersonEntity';

export class SupplierEntity extends AuditTableEntity
{
    public SupplierCod: string = "";
    public PersonCod: string = "";

    public Person : PersonEntity = new PersonEntity();

    public constructor()
    {
        super();
    }

}
