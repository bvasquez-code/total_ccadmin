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
import { PucharseLotReceptionDto } from '../../model/dto/PucharseLotReceptionDto';
import { PucharseDetLotConfirmDto } from '../../model/dto/PucharseDetLotConfirmDto';
import { PucharsePrintService } from '../../service/PucharsePrintService';

@Component({
  selector: 'app-confirmpucharse',
  templateUrl: './confirmpucharse.component.html'
})
export class ConfirmpucharseComponent implements IRegisterForm<PucharseRegisterDto,string>,ActionModalConfirmService{

  @ViewChild('txtProductCod') txtProductCod!: ElementRef<HTMLInputElement>;
  @ViewChild('txtNumUnit') txtNumUnit!: ElementRef<HTMLInputElement>;
  @ViewChild('txtLotNumUnit') txtLotNumUnit!: ElementRef<HTMLInputElement>;
  @ViewChild('txtLotNumber') txtLotNumber!: ElementRef<HTMLInputElement>;
  @ViewChild('txtExpirationDate') txtExpirationDate!: ElementRef<HTMLInputElement>;
  @ViewChild('btnCloseLotReceptionModal') btnCloseLotReceptionModal!: ElementRef<HTMLButtonElement>;
  
  
  PucharseReqCod : string = "";
  PucharseCod : string = "";
  PucharseRegister : PucharseRegisterDto = new PucharseRegisterDto();
  PucharseDetails : PucharseDetailsDto = new PucharseDetailsDto();
  Store : StoreEntity = new StoreEntity();
  WarehouseList : WarehouseEntity[] = [];
  pucharseDetSelect : PucharseDetEntity = new PucharseDetEntity();
  IsReceptionWithLots: boolean = false;
  LotReceptionList: PucharseLotReceptionDto[] = [];
  private readonly maxLotNumberLength: number = 32;

  constructor(
    private pucharseService : PucharseService,
    private productService : ProductService,
    private pucharseDetService : PucharseDetService,
    private dataSesionService : DataSesionService,
    private router: Router,
    private toastrService : ToastrService,
    private pucharsePrintService: PucharsePrintService
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
    await this.confirmDetail(pucharseDet);
  }

  private async confirmDetail(pucharseDet : PucharseDetEntity): Promise<boolean>{
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

      return true;
    }

    return false;
    
  }

  selectRowTable(pucharseDet : PucharseDetEntity)
  {
    this.pucharseDetSelect = pucharseDet;
    this.txtNumUnit.nativeElement.value = String(pucharseDet.NumUnit);
  }

  selectRowTableLots(pucharseDet : PucharseDetEntity)
  {
    this.pucharseDetSelect = pucharseDet;
    this.LotReceptionList = [];
    this.clearLotReceptionForm();
  }

  addLotReceptionLine(): void
  {
    try {
      const numUnit = Number(this.txtLotNumUnit.nativeElement.value);
      const lotNumber = this.txtLotNumber.nativeElement.value.trim();
      const expirationDate = this.txtExpirationDate.nativeElement.value;

      if (!numUnit || numUnit <= 0) {
        throw new Error("Ingrese una cantidad valida");
      }

      if (!lotNumber) {
        throw new Error("Ingrese el lote");
      }

      if (lotNumber.length > this.maxLotNumberLength) {
        throw new Error("El lote no puede superar 32 caracteres");
      }

      if (!expirationDate) {
        throw new Error("Ingrese la fecha de vencimiento");
      }

      if ((this.getLotReceptionTotal() + numUnit) > this.pucharseDetSelect.NumUnit) {
        throw new Error("La cantidad recepcionada no puede superar la cantidad solicitada");
      }

      const existingLot = this.LotReceptionList.find(e => e.LotNumber === lotNumber && e.ExpirationDate === expirationDate);

      if (existingLot) {
        existingLot.NumUnit += numUnit;
      } else {
        this.LotReceptionList.push(new PucharseLotReceptionDto({
          NumUnit: numUnit,
          LotNumber: lotNumber,
          ExpirationDate: expirationDate
        }));
      }

      this.clearLotReceptionForm();
    } catch (e: any) {
      this.toastrService.error(e.message);
    }
  }

  removeLotReceptionLine(index: number): void
  {
    this.LotReceptionList.splice(index, 1);
  }

  getLotReceptionTotal(): number
  {
    return this.LotReceptionList.reduce((total, item) => total + Number(item.NumUnit || 0), 0);
  }

  getLotReceptionPending(): number
  {
    return Number(this.pucharseDetSelect.NumUnit || 0) - this.getLotReceptionTotal();
  }

  async ConfirmLotReception(): Promise<void>
  {
    try {
      if (this.LotReceptionList.length === 0) {
        throw new Error("Debe agregar al menos un lote");
      }

      if (this.getLotReceptionTotal() < this.pucharseDetSelect.NumUnit) {
        this.toastrService.warning("La cantidad recepcionada es menor a la cantidad solicitada");
      }

      const pucharseDetLotConfirmDto = new PucharseDetLotConfirmDto();
      pucharseDetLotConfirmDto.pucharseDet = this.pucharseDetSelect;
      pucharseDetLotConfirmDto.WarehouseCod = this.WarehouseList[0].WarehouseCod;
      pucharseDetLotConfirmDto.lotDetailList = this.LotReceptionList.map(item => this.createLotDetail(item));

      const rpt = await this.pucharseDetService.ConfirmWithLots(pucharseDetLotConfirmDto);

      if (!rpt.ErrorStatus) {
        const confirmedDetailList: PucharseDetEntity[] = rpt.Data.lotDetailList || pucharseDetLotConfirmDto.lotDetailList;
        this.replaceReceptionLine(this.pucharseDetSelect, confirmedDetailList);
        this.LotReceptionList = [];
        this.btnCloseLotReceptionModal.nativeElement.click();
      } else {
        this.toastrService.error("Ocurrio un error");
      }
    } catch (e: any) {
      this.toastrService.error(e.message);
    }
  }

  private createLotDetail(item: PucharseLotReceptionDto): PucharseDetEntity
  {
    const detail = new PucharseDetEntity();
    detail.PucharseCod = this.pucharseDetSelect.PucharseCod;
    detail.ItemNumber = this.pucharseDetSelect.ItemNumber;
    detail.ProductCod = this.pucharseDetSelect.ProductCod;
    detail.Variant = this.pucharseDetSelect.Variant;
    detail.NumUnit = item.NumUnit;
    detail.NumUnitDelivered = item.NumUnit;
    detail.NumUnitPrice = this.pucharseDetSelect.NumUnitPrice;
    detail.NumTotalPrice = Number(this.pucharseDetSelect.NumUnitPrice || 0) * item.NumUnit;
    detail.IsKardexAffected = "S";
    detail.LotNumber = item.LotNumber;
    detail.ExpirationDate = item.ExpirationDate;
    detail.Product = this.pucharseDetSelect.Product;

    return detail;
  }

  private replaceReceptionLine(origin: PucharseDetEntity, confirmedDetailList: PucharseDetEntity[]): void
  {
    const index = this.PucharseRegister.DetailList.findIndex(item => this.sameDetailLine(item, origin));
    const detailList = confirmedDetailList.map(item => {
      item.Product = origin.Product;
      item.NumUnitDelivered = item.NumUnitDelivered || item.NumUnit;
      item.IsKardexAffected = "S";
      return item;
    });

    if (index >= 0) {
      this.PucharseRegister.DetailList.splice(index, 1, ...detailList);
    }
  }

  private clearLotReceptionForm(): void
  {
    setTimeout(() => {
      if (this.txtLotNumUnit) this.txtLotNumUnit.nativeElement.value = "";
      if (this.txtLotNumber) this.txtLotNumber.nativeElement.value = "";
      if (this.txtExpirationDate) this.txtExpirationDate.nativeElement.value = "";
    });
  }

  print(): void
  {
    const receivedRows = this.PucharseRegister.DetailList.filter(item => item.IsKardexAffected === "S");

    if (receivedRows.length === 0) {
      this.toastrService.error("No hay productos recibidos para imprimir");
      return;
    }

    this.pucharsePrintService.printReception(this.PucharseDetails, receivedRows, this.Store, this.WarehouseList);
  }

  async EndReception()
  {
    const rpt = await this.pucharseService.EndReception(this.PucharseDetails.Headboard);

    if(!rpt.ErrorStatus){
      this.toastrService.success("Operación realizada con exito");
      const receivedRows = this.PucharseRegister.DetailList.filter(item => item.IsKardexAffected === "S");
      this.pucharsePrintService.printReception(this.PucharseDetails, receivedRows, this.Store, this.WarehouseList);
      setTimeout(() => {
        this.router.navigate(['/enterprise/pucharse/pages/listreception']);
      }, 1000);
    }else{
      this.toastrService.error("Ocurrio un error");
    }
  }

}
