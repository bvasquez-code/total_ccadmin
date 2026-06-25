import { AuditTableEntity } from "src/app/enterprise/shared/model/entity/AuditTableEntity";
import { TransferHeadEntity } from "./TransferHeadEntity";

export class TransferRequestHeadEntity extends AuditTableEntity {
    public TransferReqCod: string;
    public TypeOperation: string;
    public StoreCodOrigin: string;
    public StoreCodDest: string;
    public StoreCodRequestedBy: string;
    public TransferStatus: string;
    public DispatchDate?: Date | any;
    public ArrivalDate?: Date | any;
    public UserOriginConfirm: string;
    public DateOriginConfirm?: Date | any;
    public UserDestConfirm: string;
    public DateDestConfirm?: Date | any;
    public Observation: string;

    constructor() {
        super();
        this.TransferReqCod = '';
        this.TypeOperation = '';
        this.StoreCodOrigin = '';
        this.StoreCodDest = '';
        this.StoreCodRequestedBy = '';
        this.TransferStatus = '';
        this.DispatchDate = null;
        this.ArrivalDate = null;
        this.UserOriginConfirm = '';
        this.DateOriginConfirm = null;
        this.UserDestConfirm = '';
        this.DateDestConfirm = null;
        this.Observation = '';
    }

    public buildTransferHead(): TransferHeadEntity {
        const entity = new TransferHeadEntity();
        entity.TransferCod = this.TransferReqCod;
        entity.TypeOperation = this.TypeOperation;
        entity.StoreCodOrigin = this.StoreCodOrigin;
        entity.StoreCodDest = this.StoreCodDest;
        entity.StoreCodRequestedBy = this.StoreCodRequestedBy;
        entity.TransferStatus = this.TransferStatus;
        entity.DispatchDate = this.DispatchDate;
        entity.ArrivalDate = this.ArrivalDate;
        entity.UserOriginConfirm = this.UserOriginConfirm;
        entity.DateOriginConfirm = this.DateOriginConfirm;
        entity.UserDestConfirm = this.UserDestConfirm;
        entity.DateDestConfirm = this.DateDestConfirm;
        entity.Observation = this.Observation;
        return entity;
    }
}
