import { ClientEntity } from "src/app/enterprise/client/model/entity/ClientEntity";
import { CreditNoteHeadEntity } from "../entity/CreditNoteHeadEntity";
import { CreditNoteDetDto } from "./CreditNoteDetDto";
import { CreditNoteDocumentEntity } from "../entity/CreditNoteDocumentEntity";
import { SalePaymentEntity } from "src/app/enterprise/trxpayment/model/entity/SalePaymentEntity";
import { SaleDocumentEntity } from "../entity/SaleDocumentEntity";
import { CreditNoteApplicationEntity } from "../entity/CreditNoteApplicationEntity";
import { SaleBillingEntity } from "../entity/SaleBillingEntity";

export class CreditNoteDetailDto {

    public Client: ClientEntity;
    public Headboard: CreditNoteHeadEntity;
    public Document: CreditNoteDocumentEntity;
    public DocumentReference: SaleDocumentEntity;
    public DetailList: CreditNoteDetDto[];
    public DetailPayment: SalePaymentEntity[];
    public ApplicationList: CreditNoteApplicationEntity[];
    public NumAvailableBalance: number;
    public SaleBilling: SaleBillingEntity;

    constructor() {
        this.Client = new ClientEntity();
        this.Headboard = new CreditNoteHeadEntity();
        this.Document = new CreditNoteDocumentEntity();
        this.DetailList = [];
        this.DetailPayment = [];
        this.ApplicationList = [];
        this.NumAvailableBalance = 0;
        this.DocumentReference = new SaleDocumentEntity();
        this.SaleBilling = new SaleBillingEntity();
    }
}
