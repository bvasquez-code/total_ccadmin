import { AuditTableEntity } from "src/app/enterprise/shared/model/entity/AuditTableEntity";

export class ProductTaxConfigEntity extends AuditTableEntity {
    public ProductTaxConfigId?: number;
    public ProductCod: string = "";
    public StoreCod: string = "";
    public TaxCod: string = "";
    public TaxAffectationCod: string = "";
    public IsMainTax: string = "N";
    public TaxRateValue: number = 0;
    public FixedUnitAmount: number = 0;
    public TaxCalculationType: string = "P";
    public IsInformative: string = "N";
    public CalculationOrder: number = 100;
}
