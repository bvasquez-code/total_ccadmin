import { ProductEntity } from '../entity/ProductEntity';
import { ProductConfigEntity } from '../entity/ProductConfigEntity';
import { ProductVariantEntity } from '../entity/ProductVariantEntity';
import { ProductInfoWarehouseEntity } from '../entity/ProductInfoWarehouseEntity';
import { ProductInfoEntity } from '../entity/ProductInfoEntity';
import { ProductPictureEntity } from '../entity/ProductPictureEntity';

export class ProductInfoDto{

    public Product : ProductEntity;
    public Config : ProductConfigEntity;
    public VariantList : ProductVariantEntity[];
    public InfoList : ProductInfoEntity[];
    public InfoWarehouseList : ProductInfoWarehouseEntity[];
    public Picture : ProductPictureEntity;

    constructor()
    {
        this.Product = new ProductEntity();
        this.Config = new ProductConfigEntity();
        this.VariantList = [];
        this.InfoList = [];
        this.InfoWarehouseList = [];
        this.Picture = new ProductPictureEntity();
    }

    SetDataSession( DataSession : any )
    {
        this.Product =  DataSession.Product;
        this.Config =  DataSession.Config;
        this.VariantList =  DataSession.VariantList;
        this.InfoList =  DataSession.InfoList;
        this.InfoWarehouseList =  DataSession.InfoWarehouseList;
        this.Picture =  DataSession.Picture;
    }

}