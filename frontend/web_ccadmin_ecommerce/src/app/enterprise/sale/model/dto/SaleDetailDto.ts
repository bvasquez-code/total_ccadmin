import { TrxPaymentEntity } from '../entity/TrxPaymentEntity';
import { SaleBillingEntity } from '../entity/SaleBillingEntity';

export interface SalePaymentDto {
  PaymentNumber: number;
  SaleCod: string;
  TrxPaymentId: number;
  CurrencyCod: string;
  NumAmountPaid: number;
  NumAmountReturned: number;
  Status: string;
  TrxPayment: TrxPaymentEntity;
}

export interface SaleDetailDto {
  Headboard: {
    SaleCod: string;
    PresaleCod: string;
    StoreCod: string;
    ClientCod: string;
    CurrencyCod: string;
    NumExchangevalue: number;
    NumTotalPrice: number;
    SaleStatus: string;
    IsPaid: string;
  };
  DetailList: unknown[];
  DetailPayment: SalePaymentDto[];
  SaleBilling: SaleBillingEntity | null;
}
