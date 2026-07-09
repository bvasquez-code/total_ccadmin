export class StoreSequenceEntity {

    public StoreCod: string;
    public PeriodId: number;
    public SequenceTrx: number;
    public Prefix: string;
    public SequenceTableType: string;
    public SequenceLength: number;

    constructor() {
        this.StoreCod = "";
        this.PeriodId = 0;
        this.SequenceTrx = 0;
        this.Prefix = "";
        this.SequenceTableType = "";
        this.SequenceLength = 7;
    }
}
