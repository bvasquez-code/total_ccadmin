export class SaleWebOrderDto {
  public SaleCod: string = '';
  public PresaleCod: string = '';
  public ClientCod: string = '';
  public ClientName: string = '';
  public NumTotalPrice: number = 0;
  public CreationDate: Date = new Date();
  public SaleStatus: string = '';
  public IsPaid: string = '';
  public HasFiscalDocument: string = '';
  public HasCreditNote: string = '';
  public DeliveryTypeCod: string = '';
  public DeliveryTypeName: string = '';
  public DeliveryStatus: string = '';
}
