import { ClientEntity } from "src/app/enterprise/client/model/entity/ClientEntity";
import { CreditNoteHeadEntity } from "../entity/CreditNoteHeadEntity";
import { CreditNoteDocumentEntity } from "../entity/CreditNoteDocumentEntity";

export class CreditNoteHeadDto {

    public CreditNoteHead: CreditNoteHeadEntity;
    public Client: ClientEntity;
    public CreditNoteDocument : CreditNoteDocumentEntity;

    constructor() {
        this.CreditNoteHead = new CreditNoteHeadEntity();
        this.Client = new ClientEntity();
        this.CreditNoteDocument = new CreditNoteDocumentEntity();
    }
}
