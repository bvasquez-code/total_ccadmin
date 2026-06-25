import { PersonEntity } from 'src/app/enterprise/person/model/entity/PersonEntity';
import { AuditTableEntity } from '../../../shared/model/entity/AuditTableEntity';
import { UserProfileEntity } from './UserProfileEntity';
import { UserStoreEntity } from './UserStoreEntity';
export class AppUserEntity extends AuditTableEntity {
    public UserCod: string = "";
    public PersonCod: string = "";
    public PasswordDecoded: string = "";
    public Password: string = "";
    public Email: string = "";
    public CreationCode: string = "";
    public DateExpire: Date = new Date();
    public RecoveryCod: string = "";

    public Person: PersonEntity = new PersonEntity();
    public UserProfileList: UserProfileEntity[] = [];
    public UserStoreList: UserStoreEntity[] = [];

    public constructor() {
        super();
    }

}