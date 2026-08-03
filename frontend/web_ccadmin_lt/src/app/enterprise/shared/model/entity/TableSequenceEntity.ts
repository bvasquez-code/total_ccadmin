export class TableSequenceEntity {

    public SequenceTrx: number;
    public Prefix: string;
    public SequenceTableType: string;
    public length: number;
    public UsePrefix: string;

    constructor() {
        this.SequenceTrx = 0;
        this.Prefix = "";
        this.SequenceTableType = "";
        this.length = 8;
        this.UsePrefix = "S";
    }
}
