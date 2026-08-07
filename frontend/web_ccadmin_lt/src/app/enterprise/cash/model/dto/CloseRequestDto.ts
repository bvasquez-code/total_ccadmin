export class CloseRequestDto {
    public HasCashCount: string;
    public CountedCashAmount: number | null;
    public CountedOtherAmount: number | null;
    public Commenter: string;

    constructor() {
        this.HasCashCount = 'N';
        this.CountedCashAmount = null;
        this.CountedOtherAmount = null;
        this.Commenter = '';
    }
}
