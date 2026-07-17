export class TransferConstants {
    static readonly TYPE_OPERATION_REQUEST = 'TR';
    static readonly TYPE_OPERATION_SEND = 'TS';

    static readonly TRANSFER_MODE_REGULAR = 'R';
    static readonly TRANSFER_MODE_DIRECT = 'D';

    static readonly STATUS_PENDING = 'P';
    static readonly STATUS_DIRECT_DRAFT = 'T';
    static readonly STATUS_CONFIRMED = 'C';
    static readonly STATUS_DISPATCHED = 'D';
    static readonly STATUS_FINALIZED = 'F';
    static readonly STATUS_REJECTED = 'R';
    static readonly STATUS_CANCELLED = 'X';
    static readonly STATUS_APPROVED = 'A';
}
