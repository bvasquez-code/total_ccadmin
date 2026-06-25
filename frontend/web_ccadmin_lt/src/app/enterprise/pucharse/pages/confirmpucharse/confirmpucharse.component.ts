import { Component, ElementRef, ViewChild } from '@angular/core';
import { PucharseRegisterDto } from '../../model/dto/PucharseRegisterDto';
import { IRegisterForm } from 'src/app/enterprise/shared/interface/IRegisterForm';
import { Router } from '@angular/router';
import { PucharseService } from '../../service/PucharseService';
import { ToastrService } from 'ngx-toastr';
import { ProductService } from 'src/app/enterprise/product/service/product.service';
import { PucharseDetailsDto } from '../../model/dto/PucharseDetailsDto';
import { ProductEntity } from 'src/app/enterprise/product/model/entity/ProductEntity';
import { PucharseDetEntity } from '../../model/entity/PucharseDetEntity';
import { DataSesionService } from 'src/app/enterprise/compartido/service/datasesion.service';
import { PucharseDetService } from '../../service/PucharseDetService';
import { PucharseDetConfirmDto } from '../../model/dto/PucharseDetConfirmDto';
import { StoreEntity } from 'src/app/enterprise/shared/model/entity/StoreEntity';
import { WarehouseEntity } from 'src/app/enterprise/shared/model/entity/WarehouseEntity';
import { ActionModalConfirmService } from 'src/app/enterprise/shared/interface/ActionModalConfirmService';

@Component({
  selector: 'app-confirmpucharse',
  templateUrl: './confirmpucharse.component.html'
})
export class ConfirmpucharseComponent implements IRegisterForm<PucharseRegisterDto,string>,ActionModalConfirmService{

  @ViewChild('txtProductCod') txtProductCod!: ElementRef<HTMLInputElement>;
  @ViewChild('txtNumUnit') txtNumUnit!: ElementRef<HTMLInputElement>;
  
  
  PucharseReqCod : string = "";
  PucharseCod : string = "";
  PucharseRegister : PucharseRegisterDto = new PucharseRegisterDto();
  PucharseDetails : PucharseDetailsDto = new PucharseDetailsDto();
  Store : StoreEntity = new StoreEntity();
  WarehouseList : WarehouseEntity[] = [];
  pucharseDetSelect : PucharseDetEntity = new PucharseDetEntity();

  constructor(
    private pucharseService : PucharseService,
    private productService : ProductService,
    private pucharseDetService : PucharseDetService,
    private dataSesionService : DataSesionService,
    private router: Router,
    private toastrService : ToastrService
  )
  {
    this.GetParamUrl(this.router);
  }


  actionModal(ModalId: string): void {
   if(ModalId === "modal_confirm") this.Confirm(this.pucharseDetSelect);
   if(ModalId === "modal_end_reception") this.EndReception();
  }

  GetParamUrl(router: Router): void {
    let urlTree : any = this.router.parseUrl(this.router.url);
    this.PucharseCod =  (urlTree.queryParams['PucharseCod']) ? urlTree.queryParams['PucharseCod'] : "";
    this.FindDataForm(this.PucharseCod);
  }

  async FindDataForm(PucharseCod: string): Promise<void> {

    const rpt = await this.pucharseService.FindDataForm(PucharseCod);

    if(!rpt.ErrorStatus){
      this.PucharseDetails = rpt.DataAdditional.find( e => e.Name === "PucharseDetails" )?.Data;
      this.Store = rpt.DataAdditional.find( e => e.Name === "Store" )?.Data;
      this.WarehouseList = rpt.DataAdditional.find( e => e.Name === "WarehouseList" )?.Data;

      this.LoadingTable(this.PucharseDetails);
    }
  }

  LoadingForm(Entity: PucharseRegisterDto): void {
    throw new Error('Method not implemented.');
  }

  LoadingTable(PucharseDetails : PucharseDetailsDto){

    const DetailList: PucharseDetEntity[] = PucharseDetails.DetailList.filter( e => e.IsKardexAffected !== "S" );

    for(let item of DetailList){

      if(!item.NumUnitDelivered || item.NumUnitDelivered === 0){
        item.NumUnitDelivered = item.NumUnit;
      }
      this.PucharseRegister.DetailList.push(item);
    }

  }

  private sameDetailLine(a: PucharseDetEntity, b: { ItemNumber?: number, ProductCod?: string, Variant?: string, LotNumber?: string, ExpirationDate?: any }): boolean {
    if ((a?.ItemNumber ?? 0) > 0 && (b?.ItemNumber ?? 0) > 0) {
      return a.ItemNumber === b.ItemNumber;
    }
    return a.ProductCod === b.ProductCod
      && a.Variant === b.Variant
      && (a.LotNumber ?? '') === (b.LotNumber ?? '')
      && (a.ExpirationDate ?? '') === (b.ExpirationDate ?? '');
  }

  private findReceptionLineByProduct(productCod: string): PucharseDetEntity | undefined {
    return this.PucharseRegister.DetailList.find(e => e.ProductCod === productCod && e.IsKardexAffected !== "S")
      ?? this.PucharseRegister.DetailList.find(e => e.ProductCod === productCod);
  }

  private findOriginLineByProduct(productCod: string): PucharseDetEntity | undefined {
    return this.PucharseDetails.DetailList.find(e => e.ProductCod === productCod && e.IsKardexAffected !== "S")
      ?? this.PucharseDetails.DetailList.find(e => e.ProductCod === productCod);
  }

  Save(): Promise<void> {
    throw new Error('Method not implemented.');
  }

  async SearchProd()
  {
    try{
      let BarCode : string = this.txtProductCod.nativeElement.value;

      const rpt = await this.productService.FindByBarCode(BarCode);
  
      if(!rpt.ErrorStatus){
        const Product : ProductEntity = rpt.Data;
  
        let ProductDetailCount : PucharseDetEntity | undefined = this.findReceptionLineByProduct(Product.ProductCod);
        let ProductDetailOrigin :PucharseDetEntity | undefined = this.findOriginLineByProduct(Product.ProductCod);
  
        if(ProductDetailCount?.IsKardexAffected === "S"){
          throw new Error("Producto ya fue confirmado como ingresado");
        }

        if( ProductDetailCount )
        {
          ProductDetailCount.NumUnit = ProductDetailCount.NumUnit + 1;
          return;
        }
  
        if( !ProductDetailCount && ProductDetailOrigin)
        {
          ProductDetailCount = ProductDetailOrigin;
          ProductDetailCount.NumUnit = 1;
          ProductDetailCount.Product = Product;
          this.PucharseRegister.DetailList.push(ProductDetailCount);
          return;
        }
  
        if( !ProductDetailCount && !ProductDetailOrigin)
        {
          ProductDetailCount = new PucharseDetEntity();
          ProductDetailCount.PucharseCod = this.PucharseDetails.Headboard.PucharseCod;
          ProductDetailCount.ProductCod = Product.ProductCod;
          ProductDetailCount.Variant = '0000';
          ProductDetailCount.NumUnit = 1;
          ProductDetailCount.NumUnitPrice = 0;
          ProductDetailCount.NumTotalPrice = 0;
          ProductDetailCount.Product = Product;
          this.PucharseRegister.DetailList.push(ProductDetailCount);
          return;
        }
  
      }
    }catch(e : any){
      this.toastrService.error(e.message);
    }
    finally{
      this.txtProductCod.nativeElement.value = "";
    }
    
  }

  updateQuantity(){
    this.pucharseDetSelect.NumUnitDelivered = Number(this.txtNumUnit.nativeElement.value);
  }

  async Confirm(pucharseDet : PucharseDetEntity){
    const pucharseDetConfirmDto : PucharseDetConfirmDto = new PucharseDetConfirmDto();

    pucharseDetConfirmDto.pucharseDet = pucharseDet;
    pucharseDetConfirmDto.pucharseDetDelivery.PucharseCod = pucharseDet.PucharseCod;
    pucharseDetConfirmDto.pucharseDetDelivery.ItemNumber = pucharseDet.ItemNumber;
    pucharseDetConfirmDto.pucharseDetDelivery.ProductCod = pucharseDet.ProductCod;
    pucharseDetConfirmDto.pucharseDetDelivery.Variant = pucharseDet.Variant;
    pucharseDetConfirmDto.pucharseDetDelivery.WarehouseCod = this.WarehouseList[0].WarehouseCod;
    pucharseDetConfirmDto.pucharseDetDelivery.NumUnit = pucharseDet.NumUnitDelivered;
    pucharseDetConfirmDto.pucharseDetDelivery.LotNumber = pucharseDet.LotNumber;
    pucharseDetConfirmDto.pucharseDetDelivery.ExpirationDate = pucharseDet.ExpirationDate;

    const rpt = await this.pucharseDetService.Confirm(pucharseDetConfirmDto);

    if(!rpt.ErrorStatus){

      const pucharseDet = this.PucharseRegister.DetailList.find( e => this.sameDetailLine(e, pucharseDetConfirmDto.pucharseDetDelivery));

      if(pucharseDet){
        pucharseDet.IsKardexAffected = "S";
      }

    }
    
  }

  selectRowTable(pucharseDet : PucharseDetEntity)
  {
    this.pucharseDetSelect = pucharseDet;
    this.txtNumUnit.nativeElement.value = String(pucharseDet.NumUnit);
  }

  async EndReception()
  {
    const rpt = await this.pucharseService.EndReception(this.PucharseDetails.Headboard);

    if(!rpt.ErrorStatus){
      this.toastrService.success("Operación realizada con exito");
      setTimeout(() => {
        this.router.navigate(['/enterprise/pucharse/pages/listreception']);
      }, 1000);
    }else{
      this.toastrService.error("Ocurrio un error");
    }
  }

}
