import { AuditTableEntity } from "src/app/enterprise/shared/model/entity/AuditTableEntity";

export class TransferHeadEntity extends AuditTableEntity {
    public TransferCod: string;
    public TypeOperation: string;
    public StoreCodOrigin: string;
    public StoreCodDest: string;
    public StoreCodRequestedBy: string;
    public TransferStatus: string;
    public ReceiveStatus: string;
    public DispatchDate?: Date | any;
    public ArrivalDate?: Date | any;
    public UserOriginConfirm: string;
    public DateOriginConfirm?: Date | any;
    public UserDestConfirm: string;
    public DateDestConfirm?: Date | any;
    public Observation: string;

    constructor() {
        super();
        this.TransferCod = '';
        this.TypeOperation = '';
        this.StoreCodOrigin = '';
        this.StoreCodDest = '';
        this.StoreCodRequestedBy = '';
        this.TransferStatus = 'P';
        this.ReceiveStatus = 'P';
        this.DispatchDate = null;
        this.ArrivalDate = null;
        this.UserOriginConfirm = '';
        this.DateOriginConfirm = null;
        this.UserDestConfirm = '';
        this.DateDestConfirm = null;
        this.Observation = '';
    }
}
