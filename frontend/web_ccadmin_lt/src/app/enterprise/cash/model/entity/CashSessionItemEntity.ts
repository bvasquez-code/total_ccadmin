import { AuditTableEntity } from "src/app/enterprise/shared/model/entity/AuditTableEntity";

export class CashSessionItemEntity extends AuditTableEntity {

    public ItemID: number;

    public CashSessionID: number;

    public ItemType: string; // D/P/M

    // Denominaciones (D)
    public Denomination: number;
    public Qty: number;

    // Pago (P)
    public PaymentMethodCod: string;

    // Movimiento manual (M)
    public MovementType: string; // IN/OU
    public ReferenceCod: string;

    public Amount: number;
    public CurrencyCod: string;

    public Commenter: string;

    constructor() {
        super();
        this.ItemID = 0;

        this.CashSessionID = 0;

        this.ItemType = 'D';

        this.Denomination = 0;
        this.Qty = 0;

        this.PaymentMethodCod = '';

        this.MovementType = '';
        this.ReferenceCod = '';

        this.Amount = 0;
        this.CurrencyCod = 'PEN';

        this.Commenter = '';
    }
}
