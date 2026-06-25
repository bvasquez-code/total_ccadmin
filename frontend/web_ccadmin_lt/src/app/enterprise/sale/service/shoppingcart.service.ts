import { Injectable } from "@angular/core";
import { ProductInfoDto } from "../../product/model/dto/ProductInfoDto";
import { PresaleRegisterDto } from "../model/dto/PresaleRegisterDto";
import { PresaleDetEntity } from "../model/entity/PresaleDetEntity";
import { ProductVariantEntity } from "../../product/model/entity/ProductVariantEntity";
import { ToastrService } from "ngx-toastr";
import { ClientEntity } from '../../client/model/entity/ClientEntity';
import { PresaleDetailDto } from "../model/dto/PresaleDetailDto";

@Injectable({
    providedIn: 'root'
})

export class ShoppingCartService
{

    ShoppingCart : PresaleRegisterDto = new PresaleRegisterDto();

    constructor(private toastrService: ToastrService)
    {

    }

    private sameProductVariant(det: PresaleDetEntity, ProductCod: string, Variant: string): boolean {
        return det.ProductCod === ProductCod && det.Variant === Variant;
    }

    public Init()
    {
        this.ShoppingCart = new PresaleRegisterDto();
        let ShoppingCartStr :string | null = sessionStorage.getItem('ShoppingCart');

        if(  ShoppingCartStr )
        {
            const ShoppingCartObj = JSON.parse(ShoppingCartStr);
            this.ShoppingCart.SetDataSession( Object.assign(new PresaleRegisterDto(), ShoppingCartObj) );
        }
   
    }

    public SetCart(PresaleDetail : PresaleDetailDto)
    {
        let ShoppingCartTmp : PresaleRegisterDto = new PresaleRegisterDto();
        ShoppingCartTmp.Headboard = PresaleDetail.Headboard;
        ShoppingCartTmp.DetailList = PresaleDetail.DetailList;
        sessionStorage.setItem("ShoppingCart",JSON.stringify(ShoppingCartTmp));
        this.Init();
    }

    Clean():void
    {
        this.ShoppingCart = new PresaleRegisterDto();
        sessionStorage.setItem("ShoppingCart","");
    }

    public getCart():PresaleRegisterDto
    {
        return this.ShoppingCart;
    }

    public GetExistsStock(NumUnit : number,ProductInfo : ProductInfoDto,ProductVariant : ProductVariantEntity):number
    {
        let NumDigitalStock : number = ProductInfo.InfoList.find( e => e.Variant === ProductVariant.Variant )?.NumDigitalStock  || 0;

        if( NumUnit >  NumDigitalStock)
        {
            this.toastrService.error("No existe stock disponible para el producto");
            throw new Error('Imposible stock');
        }
        if( NumUnit <  0)
        {
            throw new Error('Imposible stock');
        }
        return NumUnit;
    }


    public addUnit(ProductInfo : ProductInfoDto,ProductVariant : ProductVariantEntity):void
    {
        let presaleDetEntity : PresaleDetEntity | undefined = this.GetProductInCart(ProductInfo.Product.ProductCod,ProductVariant.Variant);

        if(presaleDetEntity)
        {
            presaleDetEntity.Update(
                this.GetExistsStock(presaleDetEntity.NumUnit + 1,ProductInfo,ProductVariant)
            );
        }
        else
        {   presaleDetEntity = new PresaleDetEntity();
            presaleDetEntity.Build(ProductInfo,ProductVariant.Variant);
            presaleDetEntity.Update(
                this.GetExistsStock(1,ProductInfo,ProductVariant)
            );
            this.ShoppingCart.DetailList.push(presaleDetEntity);
        }

        this.ReBuild();
    }

    public HandbookUnit(ProductInfo : ProductInfoDto,ProductVariant : ProductVariantEntity, NumUnit : number):void
    {
        let presaleDetEntity : PresaleDetEntity | undefined = this.GetProductInCart(ProductInfo.Product.ProductCod,ProductVariant.Variant);
        
        if(presaleDetEntity)
        {
            if( NumUnit === 0)
            {
                this.ShoppingCart.DetailList = this.ShoppingCart.DetailList.filter( e => !this.sameProductVariant(e, ProductInfo.Product.ProductCod, ProductVariant.Variant) );
            }
            else
            {
                presaleDetEntity.Update(
                    this.GetExistsStock(NumUnit,ProductInfo,ProductVariant)
                );
            }            
        }
        else
        {   presaleDetEntity = new PresaleDetEntity();
            presaleDetEntity.Build(ProductInfo,ProductVariant.Variant);
            presaleDetEntity.Update(
                this.GetExistsStock(NumUnit,ProductInfo,ProductVariant)
            );
            this.ShoppingCart.DetailList.push(presaleDetEntity);
        }

        this.ReBuild();
    }

    public preventZeroSubtract(ProductInfo : ProductInfoDto,ProductVariant : ProductVariantEntity){

        let presaleDetEntity : PresaleDetEntity | undefined = this.GetProductInCart(ProductInfo.Product.ProductCod,ProductVariant.Variant);

        if(presaleDetEntity){
            return ( presaleDetEntity.NumUnit - 1  === 0);
        }
        return false;
    }

    public subtractUnit(ProductInfo : ProductInfoDto,ProductVariant : ProductVariantEntity):void
    {
        let presaleDetEntity : PresaleDetEntity | undefined = this.GetProductInCart(ProductInfo.Product.ProductCod,ProductVariant.Variant);

        if(presaleDetEntity)
        {
            if( presaleDetEntity.NumUnit - 1  === 0)
            {
                this.ShoppingCart.DetailList = this.ShoppingCart.DetailList.filter( e => !this.sameProductVariant(e, ProductInfo.Product.ProductCod, ProductVariant.Variant) );
            }
            else
            {
                presaleDetEntity.Update(
                    this.GetExistsStock(presaleDetEntity.NumUnit - 1,ProductInfo,ProductVariant)                
                );
            }
        }

        this.ReBuild();
    }

    public DeleteProduct(ProductCod : string):void
    {
        this.ShoppingCart.DetailList = this.ShoppingCart.DetailList.filter( e => e.ProductCod !== ProductCod );
        this.ReBuild();
    }

    public AddClient(Client : ClientEntity)
    {
        this.ShoppingCart.Headboard.Client = Client;
        this.ShoppingCart.Headboard.ClientCod = Client.ClientCod;
        this.ReBuild();
    }


    existproductInCart(ProductCod : string , Variant : string) :boolean
    {
        return (this.ShoppingCart.DetailList.filter( e => this.sameProductVariant(e, ProductCod, Variant) ).length > 0)
    }

    GetProductInCart(ProductCod : string , Variant : string) :PresaleDetEntity | undefined
    {
        return this.ShoppingCart.DetailList.find( e => this.sameProductVariant(e, ProductCod, Variant) );
    }


    GetNumUnitProd(ProductCod : string):number
    {
        let NumItem = 0;
        const listProductVariant = this.ShoppingCart.DetailList.filter( e => e.ProductCod === ProductCod );
        for(let Product of listProductVariant)
        {
            NumItem = NumItem + Product.NumUnit;
        }
        return NumItem;
    }

    GetTmpProductInCart(ProductCod : string) :PresaleDetEntity[]
    {
        let PresaleDet : PresaleDetEntity[] = this.ShoppingCart.DetailList.filter( e => e.ProductCod === ProductCod );
        return PresaleDet; 
    }

    getTotalProduct(ProductCod : string):number
    {
      let NumUnit : number = 0;
      let result = this.ShoppingCart.DetailList.filter( e => e.ProductCod === ProductCod);
      NumUnit = result.map( item => item.NumUnit).reduce((a, b) => a + b, 0);
      return NumUnit;
    }
  
    getTotalProductVariant(ProductCod : string,Variant : string):number
    {
      let NumUnit : number = 0;
      let result = this.ShoppingCart.DetailList.filter( e => this.sameProductVariant(e, ProductCod, Variant));
      NumUnit = result.map( item => item.NumUnit).reduce((a, b) => a + b, 0);
      return NumUnit;
    }

    ReBuild():void
    {
        this.ShoppingCart.ReBuild();
        this.saveCartSession();
    }

    saveCartSession():void
    {
        sessionStorage.setItem("ShoppingCart",JSON.stringify(this.ShoppingCart));
    }
}
