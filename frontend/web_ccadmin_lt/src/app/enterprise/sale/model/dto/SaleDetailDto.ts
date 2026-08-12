import { SalePaymentEntity } from "src/app/enterprise/trxpayment/model/entity/SalePaymentEntity";
import { SaleDetEntity } from "../entity/SaleDetEntity";
import { SaleHeadEntity } from "../entity/SaleHeadEntity";
import { SaleDocumentEntity } from "../entity/SaleDocumentEntity";
import { CreditNoteDetailDto } from "./CreditNoteDetailDto";
import { SaleChannelEntity } from "../entity/SaleChannelEntity";
import { SaleDeliveryEntity } from "../entity/SaleDeliveryEntity";
import { SaleBillingEntity } from "../entity/SaleBillingEntity";

export class SaleDetailDto
{
    public Headboard : SaleHeadEntity;
    public DetailList : SaleDetEntity[];
    public DetailPayment : SalePaymentEntity[];
    public SaleDocument : SaleDocumentEntity;
    public SaleDocumentList : SaleDocumentEntity[];
    public CreditNoteDetail : CreditNoteDetailDto;
    public SaleChannel: SaleChannelEntity;
    public SaleDelivery: SaleDeliveryEntity | null;
    public SaleBilling: SaleBillingEntity | null;

    public constructor()
    {
        this.Headboard = new SaleHeadEntity();
        this.DetailList = [];
        this.DetailPayment = [];
        this.SaleDocument = new SaleDocumentEntity();
        this.SaleDocumentList = [];
        this.CreditNoteDetail = new CreditNoteDetailDto();
        this.SaleChannel = new SaleChannelEntity();
        this.SaleDelivery = null;
        this.SaleBilling = null;
    }
}
