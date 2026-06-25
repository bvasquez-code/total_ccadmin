import { AuditTableEntity } from "src/app/enterprise/shared/model/entity/AuditTableEntity";

export class TransferDocumentEntity extends AuditTableEntity {
    public DocumentCod: string;
    public TypeOperation: string;
    public CounterfoilCod: string;
    public TransferCod: string;
    public DocumentRole: string;
    public ReasonTransferCod: string;
    public ReasonTransferDesc: string;
    public TransportModeCod: string;
    public DepartureUbigeo: string;
    public DepartureAddress: string;
    public ArrivalUbigeo: string;
    public ArrivalAddress: string;
    public TotalWeightKg: number;
    public NumPackages: number;
    public CarrierRuc: string;
    public CarrierName: string;
    public VehiclePlate: string;
    public DriverDocType: string;
    public DriverDocNumber: string;
    public DriverLicenseNumber: string;
    public SunatStatus: string;
    public SunatTicket: string;
    public CdrCode: string;
    public CdrDescription: string;
    public XmlHash: string;

    constructor() {
        super();
        this.DocumentCod = '';
        this.TypeOperation = '';
        this.CounterfoilCod = '';
        this.TransferCod = '';
        this.DocumentRole = '';
        this.ReasonTransferCod = '';
        this.ReasonTransferDesc = '';
        this.TransportModeCod = '';
        this.DepartureUbigeo = '';
        this.DepartureAddress = '';
        this.ArrivalUbigeo = '';
        this.ArrivalAddress = '';
        this.TotalWeightKg = 0;
        this.NumPackages = 0;
        this.CarrierRuc = '';
        this.CarrierName = '';
        this.VehiclePlate = '';
        this.DriverDocType = '';
        this.DriverDocNumber = '';
        this.DriverLicenseNumber = '';
        this.SunatStatus = '';
        this.SunatTicket = '';
        this.CdrCode = '';
        this.CdrDescription = '';
        this.XmlHash = '';
    }
}
