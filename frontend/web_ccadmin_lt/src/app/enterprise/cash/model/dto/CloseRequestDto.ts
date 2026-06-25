export class CloseRequestDto {
    public CashSessionID: number;
    public Commenter: string;

    constructor() {
        this.CashSessionID = 0;
        this.Commenter = '';
    }
}
