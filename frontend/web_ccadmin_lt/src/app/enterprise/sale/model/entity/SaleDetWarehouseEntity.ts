import { AuditTableEntity } from '../../../shared/model/entity/AuditTableEntity';

export class SaleDetWarehouseEntity extends AuditTableEntity {
    public SaleCod: string = "";
    public ItemNumber: number = 0;
    public AllocationNumber: number = 1;
    public ProductCod: string = "";
    public Variant: string = "";
    public WarehouseCod: string = "";
    public NumUnit: number = 0;
    public ProductUnitName: string = "NIU";
    public ProductUnitFactor: number = 1;
    public LotNumber: string = "";
    public ExpirationDate: Date | string | null = null;
}
