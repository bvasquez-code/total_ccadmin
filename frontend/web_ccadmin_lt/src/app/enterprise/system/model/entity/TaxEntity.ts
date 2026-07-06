import { AuditTableEntity } from "src/app/enterprise/shared/model/entity/AuditTableEntity";

export class TaxEntity extends AuditTableEntity {
    public TaxCod: string = "";
    public SunatTaxCod: string = "";
    public TaxRateValue: number = 0;
    public FixedUnitAmount: number = 0;
    public TaxCalculationType: string = "P";
    public IsInformative: string = "N";
    public CalculationOrder: number = 100;
    public Name: string = "";
    public Description: string = "";
}
