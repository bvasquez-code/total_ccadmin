import { Injectable } from "@angular/core";
import { ProductInfoDto } from "../../product/model/dto/ProductInfoDto";
import { PresaleRegisterDto } from "../model/dto/PresaleRegisterDto";
import { PresaleDetEntity } from "../model/entity/PresaleDetEntity";
import { ProductVariantEntity } from "../../product/model/entity/ProductVariantEntity";
import { ToastrService } from "ngx-toastr";
import { ClientEntity } from '../../client/model/entity/ClientEntity';
import { PresaleDetailDto } from "../model/dto/PresaleDetailDto";
import { ProductUnitHelper } from "../../shared/helper/ProductUnitHelper";

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

    private getProductUnitFactor(ProductInfo: ProductInfoDto): number {
        return ProductUnitHelper.normalizeFactor(ProductInfo.Config?.ProductUnitFactor || 1);
    }

    private toInternalQuantity(ProductInfo: ProductInfoDto, visibleQuantity: number): number {
        return ProductUnitHelper.toInternalQuantity(visibleQuantity, this.getProductUnitFactor(ProductInfo));
    }

    public toVisibleQuantity(internalQuantity: number, ProductUnitFactor: number): number {
        return ProductUnitHelper.toVisibleQuantity(internalQuantity, ProductUnitFactor);
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
        if( NumUnit <  0)
        {
            throw new Error('Imposible stock');
        }
        if ((ProductInfo.Config?.IsDigital || "N").trim().toUpperCase() === "S") {
            return NumUnit;
        }

        let NumDigitalStock : number = ProductInfo.InfoList.find( e => e.Variant === ProductVariant.Variant )?.NumDigitalStock  || 0;
        if( NumUnit >  NumDigitalStock)
        {
            this.toastrService.error("No existe stock disponible para el producto");
            throw new Error('Imposible stock');
        }
        return NumUnit;
    }


    public addUnit(ProductInfo : ProductInfoDto,ProductVariant : ProductVariantEntity):void
    {
        let presaleDetEntity : PresaleDetEntity | undefined = this.GetProductInCart(ProductInfo.Product.ProductCod,ProductVariant.Variant);
        const ProductUnitFactor = this.getProductUnitFactor(ProductInfo);

        if(presaleDetEntity)
        {
            presaleDetEntity.Update(
                this.GetExistsStock(presaleDetEntity.NumUnit + ProductUnitFactor,ProductInfo,ProductVariant)
            );
        }
        else
        {   presaleDetEntity = new PresaleDetEntity();
            presaleDetEntity.Build(ProductInfo,ProductVariant.Variant);
            presaleDetEntity.Update(
                this.GetExistsStock(ProductUnitFactor,ProductInfo,ProductVariant)
            );
            this.ShoppingCart.DetailList.push(presaleDetEntity);
        }

        this.ReBuild();
    }

    public HandbookUnit(ProductInfo : ProductInfoDto,ProductVariant : ProductVariantEntity, NumUnit : number):void
    {
        let presaleDetEntity : PresaleDetEntity | undefined = this.GetProductInCart(ProductInfo.Product.ProductCod,ProductVariant.Variant);
        const internalQuantity = this.toInternalQuantity(ProductInfo, NumUnit);
        
        if(presaleDetEntity)
        {
            if( internalQuantity === 0)
            {
                this.ShoppingCart.DetailList = this.ShoppingCart.DetailList.filter( e => !this.sameProductVariant(e, ProductInfo.Product.ProductCod, ProductVariant.Variant) );
            }
            else
            {
                presaleDetEntity.Update(
                    this.GetExistsStock(internalQuantity,ProductInfo,ProductVariant)
                );
            }            
        }
        else
        {   presaleDetEntity = new PresaleDetEntity();
            presaleDetEntity.Build(ProductInfo,ProductVariant.Variant);
            presaleDetEntity.Update(
                this.GetExistsStock(internalQuantity,ProductInfo,ProductVariant)
            );
            this.ShoppingCart.DetailList.push(presaleDetEntity);
        }

        this.ReBuild();
    }

    public setManualDiscount(
        ProductInfo: ProductInfoDto,
        ProductVariant: ProductVariantEntity,
        discountInput: number
    ): void {
        const detail = this.GetProductInCart(ProductInfo.Product.ProductCod, ProductVariant.Variant);
        if (!detail || detail.NumUnit <= 0) {
            throw new Error("Primero agregue una cantidad del producto.");
        }

        const config = ProductInfo.Config;
        if ((config.IsDiscontable || "").trim().toUpperCase() !== "S") {
            throw new Error("Este producto no permite descuentos manuales.");
        }

        const value = Number(discountInput || 0);
        if (!Number.isFinite(value) || value < 0) {
            throw new Error("Ingrese un descuento valido.");
        }

        const discountType = (config.DiscountType || "").trim().toUpperCase();
        const configuredMaximum = Number(config.NumDiscountMax || 0);
        const factor = this.getProductUnitFactor(ProductInfo);
        let internalUnitDiscount = 0;

        if (discountType === "MP") {
            if (configuredMaximum <= 0 || configuredMaximum > 100) {
                throw new Error("La configuracion porcentual del producto no es valida.");
            }
            if (value > configuredMaximum) {
                throw new Error(`El descuento maximo permitido es ${configuredMaximum}%.`);
            }
            internalUnitDiscount = detail.NumUnitPrice * value / 100;
        } else if (discountType === "MF") {
            const visibleMaximum = this.toMoney(configuredMaximum * factor);
            if (configuredMaximum <= 0) {
                throw new Error("La configuracion de monto fijo del producto no es valida.");
            }
            if (value > visibleMaximum) {
                throw new Error(`El descuento maximo permitido es ${visibleMaximum}.`);
            }
            internalUnitDiscount = value / factor;
        } else {
            throw new Error("El tipo de descuento del producto no es valido.");
        }

        internalUnitDiscount = this.toMoney(internalUnitDiscount);
        if (internalUnitDiscount > detail.NumUnitPrice) {
            throw new Error("El descuento no puede superar el precio del producto.");
        }
        detail.SetDiscount(internalUnitDiscount);
        this.ReBuild();
    }

    public preventZeroSubtract(ProductInfo : ProductInfoDto,ProductVariant : ProductVariantEntity){

        let presaleDetEntity : PresaleDetEntity | undefined = this.GetProductInCart(ProductInfo.Product.ProductCod,ProductVariant.Variant);
        const ProductUnitFactor = this.getProductUnitFactor(ProductInfo);

        if(presaleDetEntity){
            return ( presaleDetEntity.NumUnit - ProductUnitFactor  === 0);
        }
        return false;
    }

    public subtractUnit(ProductInfo : ProductInfoDto,ProductVariant : ProductVariantEntity):void
    {
        let presaleDetEntity : PresaleDetEntity | undefined = this.GetProductInCart(ProductInfo.Product.ProductCod,ProductVariant.Variant);
        const ProductUnitFactor = this.getProductUnitFactor(ProductInfo);

        if(presaleDetEntity)
        {
            if( presaleDetEntity.NumUnit - ProductUnitFactor  === 0)
            {
                this.ShoppingCart.DetailList = this.ShoppingCart.DetailList.filter( e => !this.sameProductVariant(e, ProductInfo.Product.ProductCod, ProductVariant.Variant) );
            }
            else
            {
                presaleDetEntity.Update(
                    this.GetExistsStock(presaleDetEntity.NumUnit - ProductUnitFactor,ProductInfo,ProductVariant)                
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

    getTotalProductVisible(ProductCod : string):number
    {
      return this.ShoppingCart.DetailList
        .filter(e => e.ProductCod === ProductCod)
        .map(item => this.toVisibleQuantity(item.NumUnit, item.ProductUnitFactor))
        .reduce((a, b) => a + b, 0);
    }
  
    getTotalProductVariantVisible(ProductCod : string,Variant : string):number
    {
      return this.ShoppingCart.DetailList
        .filter(e => this.sameProductVariant(e, ProductCod, Variant))
        .map(item => this.toVisibleQuantity(item.NumUnit, item.ProductUnitFactor))
        .reduce((a, b) => a + b, 0);
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

    private toMoney(value: number): number
    {
        return Math.round((Number(value || 0) + Number.EPSILON) * 100) / 100;
    }
}
