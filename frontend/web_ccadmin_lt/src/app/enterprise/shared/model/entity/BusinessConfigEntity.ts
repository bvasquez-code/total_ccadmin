export class BusinessConfigEntity {
    [key: string]: any;

    public GroupId: number;
    public GroupCod: string;
    public ConfigCorr: number;
    public ConfigCod: string;
    public ConfigVal: string;
    public ConfigName: string;
    public ConfigDesc: string;
    public Str1Config: string;
    public Str2Config: string;
    public Str3Config: string;
    public Str4Config: string;
    public Num1Config: number;
    public Num2Config: number;
    public Num3Config: number;
    public Num4Config: number;
    public Dcm1Config: number;
    public Dcm2Config: number;
    public Dcm3Config: number;
    public Dcm4Config: number;
    public Sta1Config: string;
    public Sta2Config: string;
    public Sta3Config: string;
    public Sta4Config: string;
    public Status: string;

    constructor() {
        this.GroupId = 0;
        this.GroupCod = '';
        this.ConfigCorr = 0;
        this.ConfigCod = '';
        this.ConfigVal = '';
        this.ConfigName = '';
        this.ConfigDesc = '';
        this.Str1Config = '';
        this.Str2Config = '';
        this.Str3Config = '';
        this.Str4Config = '';
        this.Num1Config = 0;
        this.Num2Config = 0;
        this.Num3Config = 0;
        this.Num4Config = 0;
        this.Dcm1Config = 0;
        this.Dcm2Config = 0;
        this.Dcm3Config = 0;
        this.Dcm4Config = 0;
        this.Sta1Config = '';
        this.Sta2Config = '';
        this.Sta3Config = '';
        this.Sta4Config = '';
        this.Status = '';
    }
}
