import { AuditTableEntity } from '../../../shared/model/entity/AuditTableEntity';

export class CreditNoteDetTaxEntity extends AuditTableEntity
{
    public CreditNoteCod: string = "";
    public ItemNumber: number = 0;
    public TaxLineNumber: number = 0;
    public TaxCod: string = "";
    public SunatTaxCod: string = "";
    public TaxName: string = "";
    public TaxAffectationCod: string = "";
    public TaxAffectationName: string = "";
    public TaxCalculationType: string = "";
    public IsInformative: string = "";
    public TaxRateValue: number = 0;
    public FixedUnitAmount: number = 0;
    public TaxBaseAmount: number = 0;
    public TaxQuantity: number = 0;
    public TaxAmount: number = 0;
    public CalculationOrder: number = 0;

    public constructor()
    {
        super();
    }
}
