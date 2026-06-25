import { SalePaymentEntity } from "src/app/enterprise/trxpayment/model/entity/SalePaymentEntity";
import { SaleDetEntity } from "../entity/SaleDetEntity";
import { SaleHeadEntity } from "../entity/SaleHeadEntity";
import { SaleDocumentEntity } from "../entity/SaleDocumentEntity";
import { CreditNoteDetailDto } from "./CreditNoteDetailDto";

export class SaleDetailDto
{
    public Headboard : SaleHeadEntity;
    public DetailList : SaleDetEntity[];
    public DetailPayment : SalePaymentEntity[];
    public SaleDocument : SaleDocumentEntity;
    public CreditNoteDetail : CreditNoteDetailDto;

    public constructor()
    {
        this.Headboard = new SaleHeadEntity();
        this.DetailList = [];
        this.DetailPayment = [];
        this.SaleDocument = new SaleDocumentEntity();
        this.CreditNoteDetail = new CreditNoteDetailDto();
    }
}