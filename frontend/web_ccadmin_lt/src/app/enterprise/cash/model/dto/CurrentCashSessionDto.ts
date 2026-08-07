import { CashRegisterEntity } from "../entity/CashRegisterEntity";
import { CashSessionEntity } from "../entity/extends AuditTableEntity";

export class CurrentCashSessionDto {
    public CashRegister: CashRegisterEntity | null;
    public CashSession: CashSessionEntity | null;
    public IsOpen: boolean;

    constructor() {
        this.CashRegister = null;
        this.CashSession = null;
        this.IsOpen = false;
    }
}
