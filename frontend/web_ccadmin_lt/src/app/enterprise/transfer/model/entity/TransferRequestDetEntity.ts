import { ProductEntity } from "src/app/enterprise/product/model/entity/ProductEntity";
import { AuditTableEntity } from "src/app/enterprise/shared/model/entity/AuditTableEntity";
import { TransferDetEntity } from "./TransferDetEntity";

export class TransferRequestDetEntity extends AuditTableEntity {
    public TransferReqCod: string;
    public TypeOperation: string;
    public ProductCod: string;
    public Variant: string;
    public ItemNumber: number;
    public WarehouseCodOrigin: string
    public WarehouseCodDest: string;
    public NumUnit: number;
    public ProductUnitName: string;
    public ProductUnitFactor: number;
    public NumUnitDispatch: number;
    public NumUnitReception: number;
    public LotNumber: string;
    public ExpirationDate?: Date | any;

    public Product: ProductEntity;

    constructor() {
        super();
        this.TransferReqCod = '';
        this.TypeOperation = '';
        this.ProductCod = '';
        this.Variant = '';
        this.ItemNumber = 0;
        this.WarehouseCodOrigin = '';
        this.WarehouseCodDest = '';
        this.NumUnit = 0;
        this.ProductUnitName = 'NIU';
        this.ProductUnitFactor = 1;
        this.NumUnitDispatch = 0;
        this.NumUnitReception = 0;
        this.LotNumber = '';
        this.ExpirationDate = null;
        this.Product = new ProductEntity();
    }

    public buildTransferDet(): TransferDetEntity {
        const entity = new TransferDetEntity();
        entity.TransferCod = this.TransferReqCod;
        entity.TypeOperation = this.TypeOperation;
        entity.ProductCod = this.ProductCod;
        entity.Variant = this.Variant;
        entity.ItemNumber = this.ItemNumber;
        entity.WarehouseCodOrigin = this.WarehouseCodOrigin;
        entity.WarehouseCodDest = this.WarehouseCodDest;
        entity.NumUnit = this.NumUnit;
        entity.ProductUnitName = this.ProductUnitName;
        entity.ProductUnitFactor = this.ProductUnitFactor;
        entity.NumUnitDispatch = this.NumUnitDispatch;
        entity.NumUnitReception = this.NumUnitReception;
        entity.LotNumber = this.LotNumber;
        entity.ExpirationDate = this.ExpirationDate;
        entity.Product = this.Product;
        return entity;
    }
}
