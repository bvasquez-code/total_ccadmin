import { AuditTableEntity } from "src/app/enterprise/shared/model/entity/AuditTableEntity";

export class PersonEntity extends AuditTableEntity
{
    public PersonCod: string = "";
	public PersonType: string = "";
	public DocumentType: string = "";
	public DocumentNum: string = "";
	public Names: string = "";
	public LastNames: string = "";
	public CommercialName: string = "";
	public BusinessName: string = "";
	public Address: string = "";
	public UbigeoCod: string = "";
	public Phone: string = "";
	public CellPhone: string = "";
	public Email: string = "";


    public constructor()
    {
        super();
    }

}
