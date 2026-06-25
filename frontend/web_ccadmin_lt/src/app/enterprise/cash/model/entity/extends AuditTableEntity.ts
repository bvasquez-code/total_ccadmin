import { AuditTableEntity } from "src/app/enterprise/shared/model/entity/AuditTableEntity";

export class CashSessionEntity extends AuditTableEntity {

    public CashSessionID: number;

    public RegisterCod: string;
    public StoreCod: string;
    public UserCod: string;

    public CurrencyCod: string;

    public OpenDate: Date;
    public CloseDate: Date;

    public OpeningFloatAmount: number;

    public ExpectedCashAmount: number;
    public ExpectedOtherAmount: number;
    public ExpectedTotalAmount: number;

    public CountedCashAmount: number;
    public CountedOtherAmount: number;
    public CountedTotalAmount: number;

    public DifferenceAmount: number;

    public SessionStatus: string; // O/C/X
    public IsOpen: number;        // 1/0

    public Commenter: string;

    constructor() {
        super();
        this.CashSessionID = 0;

        this.RegisterCod = '';
        this.StoreCod = '';
        this.UserCod = '';

        this.CurrencyCod = 'PEN';

        this.OpenDate = new Date(0);
        this.CloseDate = new Date(0);

        this.OpeningFloatAmount = 0;

        this.ExpectedCashAmount = 0;
        this.ExpectedOtherAmount = 0;
        this.ExpectedTotalAmount = 0;

        this.CountedCashAmount = 0;
        this.CountedOtherAmount = 0;
        this.CountedTotalAmount = 0;

        this.DifferenceAmount = 0;

        this.SessionStatus = 'O';
        this.IsOpen = 1;

        this.Commenter = '';
    }
}
