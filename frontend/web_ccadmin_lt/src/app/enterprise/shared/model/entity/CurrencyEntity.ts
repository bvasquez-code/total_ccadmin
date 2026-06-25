import { AuditTableEntity } from './AuditTableEntity';
export class CurrencyEntity extends AuditTableEntity
{
    public CurrencyCod : string = "";
    public CurrencyAbbr : string = "";
    public CurrencySymbol : string = "";
    public CurrencyName : string = "";
    public CurrencyDesc : string = "";
    public IsCurrencySystem : string = "";
    public NumExchangevalue : number = 0;

    public constructor()
    {
        super();
    }
}
