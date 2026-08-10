import { AuditTableEntity } from "src/app/enterprise/shared/model/entity/AuditTableEntity";

export class StoreVirtualConfigEntity extends AuditTableEntity
{
    public StoreCod: string = "";
    public AllowsAutomaticDelivery: string = "N";
    public AutomaticDeliveryRadiusKm: number | null = null;
    public AllowsScheduledDelivery: string = "N";
    public ScheduledDeliveryMaxRadiusKm: number | null = null;
    public AllowsStorePickup: string = "N";
    public PreparationTimeMinutes: number = 0;

    public constructor()
    {
        super();
    }
}
