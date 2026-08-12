import { SaleBillingEntity } from '../entity/SaleBillingEntity';

export class SaleDocumentIssueDto {
    public SaleCod: string = '';
    public DocumentType: string = '';
    public SaleBilling: SaleBillingEntity = new SaleBillingEntity();
}
