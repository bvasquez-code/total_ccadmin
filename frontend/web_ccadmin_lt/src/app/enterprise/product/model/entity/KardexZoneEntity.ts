import { AuditTableEntity } from "src/app/enterprise/shared/model/entity/AuditTableEntity";

export class KardexZoneEntity extends AuditTableEntity {
    public KardexZoneID: number = 0;
    public OperationCod: string = "";
    public ItemNumber: number = 0;
    public SourceTable: string = "";
    public MovementEvent: string = "";
    public ProductCod: string = "";
    public Variant: string = "";
    public StoreCod: string = "";
    public WarehouseCod: string = "";
    public ZoneStockMoved: string = "";
    public TypeOperation: string = "";
    public NumStockMoved: number = 0;
    public NumZoneStockBefore: number = 0;
    public NumZoneStockAfter: number = 0;
    public LotNumber: string = "";
    public ExpirationDate: Date | null = null;
}
