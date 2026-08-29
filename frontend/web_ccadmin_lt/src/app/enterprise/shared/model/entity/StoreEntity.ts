import { AuditTableEntity } from "./AuditTableEntity";

export class StoreEntity extends AuditTableEntity {
    
    public StoreCod: string;
    public Name: string;
    public Description: string;
    public Address: string;
    public UbigeoCod: string;
    public SunatAddressTypeCode: string;
    public IsVirtualStoreEnabled: string;
    public Latitude: number | null;
    public Longitude: number | null;
    public CompanyCod: string;
    public CountryCod: string;

    constructor() {
        super();
        this.StoreCod = '';
        this.Name = '';
        this.Description = '';
        this.Address = '';
        this.UbigeoCod = '';
        this.SunatAddressTypeCode = '0000';
        this.IsVirtualStoreEnabled = 'N';
        this.Latitude = null;
        this.Longitude = null;
        this.CompanyCod = '';
        this.CountryCod = 'PER';
    }
}
