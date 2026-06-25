import { CreditNoteDetEntity } from "../entity/CreditNoteDetEntity";
import { CreditNoteDocumentEntity } from "../entity/CreditNoteDocumentEntity";
import { CreditNoteHeadEntity } from "../entity/CreditNoteHeadEntity";

export class CreditNoteRegisterDto {

    public Headboard: CreditNoteHeadEntity;
    public DetailList: CreditNoteDetEntity[];
    public Document : CreditNoteDocumentEntity;

    constructor() {
        this.Headboard = new CreditNoteHeadEntity();
        this.Document = new CreditNoteDocumentEntity();
        this.DetailList = [];
    }
}
