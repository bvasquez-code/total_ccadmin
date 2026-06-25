import { ClientEntity } from "src/app/enterprise/client/model/entity/ClientEntity";
import { CreditNoteHeadEntity } from "../entity/CreditNoteHeadEntity";
import { CreditNoteDetDto } from "./CreditNoteDetDto";
import { CreditNoteDocumentEntity } from "../entity/CreditNoteDocumentEntity";
import { SalePaymentEntity } from "src/app/enterprise/trxpayment/model/entity/SalePaymentEntity";
import { SaleDocumentEntity } from "../entity/SaleDocumentEntity";

export class CreditNoteDetailDto {

    public Client: ClientEntity;
    public Headboard: CreditNoteHeadEntity;
    public Document: CreditNoteDocumentEntity;
    public DocumentReference: SaleDocumentEntity;
    public DetailList: CreditNoteDetDto[];
    public DetailPayment: SalePaymentEntity[];

    constructor() {
        this.Client = new ClientEntity();
        this.Headboard = new CreditNoteHeadEntity();
        this.Document = new CreditNoteDocumentEntity();
        this.DetailList = [];
        this.DetailPayment = [];
        this.DocumentReference = new SaleDocumentEntity();
    }
}
