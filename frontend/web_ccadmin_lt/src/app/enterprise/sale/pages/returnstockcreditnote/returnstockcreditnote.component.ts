import { Component, ElementRef, ViewChild } from '@angular/core';
import { SaleService } from '../../service/sale.service';
import { CreditNoteRegisterDto } from '../../model/dto/CreditNoteRegisterDto';
import { Router } from '@angular/router';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { CreditNoteDetailDto } from '../../model/dto/CreditNoteDetailDto';
import { SaleDetailDto } from '../../model/dto/SaleDetailDto';
import { CreditNoteService } from '../../service/CreditNote.service';
import { ToastrService } from 'ngx-toastr';
import { IRegisterFormV2 } from 'src/app/enterprise/shared/interface/IRegisterFormV2';
import { CreditNoteDetDto } from '../../model/dto/CreditNoteDetDto';
import { AlertService } from 'src/app/enterprise/shared/service/AlertService';
import { ProductUnitHelper } from 'src/app/enterprise/shared/helper/ProductUnitHelper';

@Component({
  selector: 'app-returnstockcreditnote',
  templateUrl: './returnstockcreditnote.component.html'
})
export class ReturnstockcreditnoteComponent implements IRegisterFormV2<CreditNoteRegisterDto, string, CreditNoteDetailDto> {

  @ViewChild('txtDocumentCod') txtDocumentCod!: ElementRef<HTMLInputElement>;
  @ViewChild('txtCommenter') txtCommenter!: ElementRef<HTMLInputElement>;

  CreditNoteDetail: CreditNoteDetailDto = new CreditNoteDetailDto();
  CreditNoteRegister: CreditNoteRegisterDto = new CreditNoteRegisterDto();

  SaleDetail: SaleDetailDto = new SaleDetailDto();
  CreditNoteCod: string = "";
  txtDocumentCodReadOnly: boolean = false;

  constructor(
    private saleService: SaleService,
    private creditNoteService: CreditNoteService,
    private toastrService: ToastrService,
    private router: Router,
    private readonly alertService: AlertService
  ) {
    this.GetParamUrl(this.router);
    this.FindDataForm(this.CreditNoteCod);
  }


  GetParamUrl(router: Router): void {
    let urlTree: any = this.router.parseUrl(this.router.url);
    this.CreditNoteCod = (urlTree.queryParams['CreditNoteCod']) ? urlTree.queryParams['CreditNoteCod'] : "";
  }

  async FindDataForm(Id: string): Promise<void> {

    if (!Id) return;

    const rpt: ResponseWsDto = await this.creditNoteService.FindById(Id);

    if (!rpt.ErrorStatus) {
      this.txtDocumentCodReadOnly = true;
      this.CreditNoteDetail = rpt.Data;
      this.LoadingForm(this.CreditNoteDetail);
    }

  }

  async LoadingForm(CreditNoteDetail: CreditNoteDetailDto): Promise<void> {

    await this.FindBySaleCod(CreditNoteDetail.Headboard.SaleCod);
  }

  async Save(): Promise<void> {

    this.alertService.waring("¿Deseas procesar el retorno de stock? La cantidad aceptada pasará a stock físico y la diferencia saldrá definitivamente del stock. Esta acción no se puede deshacer.").then(async (result) => {
      if (result && result.isConfirmed) {

        this.CreditNoteRegister.DetailList = this.CreditNoteDetail.DetailList.map(e => e.CreditNoteDet);
        const rpt: ResponseWsDto = await this.creditNoteService.SaveReturnStock(this.CreditNoteRegister);

        if (rpt.ErrorStatus) {
          this.toastrService.error(rpt.Message);
        } else {
          this.toastrService.success("Operación realizada con exito.");
          this.router.navigate(['/enterprise/sale/pages/listcreditnote']);
        }

      }
    });


  }

  async FindByDocumentCod(): Promise<void> {

    this.SaleDetail = new SaleDetailDto();

    let DocumentCod: string = this.txtDocumentCod.nativeElement.value;

    const rpt: ResponseWsDto = await this.saleService.FindByDocumentCod(DocumentCod);

    if (rpt.ErrorStatus) return;

    if (rpt.Data) {
      this.SaleDetail = rpt.Data;
    }

  }

  async FindBySaleCod(SaleCod: string): Promise<void> {

    this.SaleDetail = new SaleDetailDto();

    const rpt: ResponseWsDto = await this.saleService.FindById(SaleCod);

    if (rpt.ErrorStatus) return;

    if (rpt.Data) {
      this.SaleDetail = rpt.Data;

      this.txtDocumentCod.nativeElement.value = this.SaleDetail.SaleDocument.DocumentCod;
      this.txtCommenter.nativeElement.value = this.CreditNoteDetail.Headboard.Commenter;
      this.CreditNoteRegister.Headboard = this.SaleDetail.CreditNoteDetail.Headboard;
      this.CreditNoteRegister.DetailList = this.SaleDetail.CreditNoteDetail.DetailList.map(e => e.CreditNoteDet);
      this.CreditNoteRegister.Document = this.SaleDetail.CreditNoteDetail.Document;
    }

  }

  AddUnit(saleDet: CreditNoteDetDto) {
    if (saleDet) {
      const productUnitFactor = ProductUnitHelper.normalizeFactor(saleDet.CreditNoteDet.ProductUnitFactor);
      if ((saleDet.CreditNoteDet.NumUnitStockReturned >= saleDet.CreditNoteDet.NumUnit)) {
        return;
      }
      saleDet.CreditNoteDet.NumUnitStockReturned = Math.min(
        saleDet.CreditNoteDet.NumUnitStockReturned + productUnitFactor,
        saleDet.CreditNoteDet.NumUnit
      );
    }
  }

  SubtractUnit(saleDet: CreditNoteDetDto) {
    if (saleDet) {
      const productUnitFactor = ProductUnitHelper.normalizeFactor(saleDet.CreditNoteDet.ProductUnitFactor);
      if ((saleDet.CreditNoteDet.NumUnitStockReturned - productUnitFactor < 0)) {
        return;
      }
      saleDet.CreditNoteDet.NumUnitStockReturned = saleDet.CreditNoteDet.NumUnitStockReturned - productUnitFactor;
    }
  }

  getUnit(saleDet: CreditNoteDetDto): number {

    if (saleDet) {
      return ProductUnitHelper.toVisibleQuantity(
        saleDet.CreditNoteDet.NumUnitStockReturned,
        saleDet.CreditNoteDet.ProductUnitFactor
      );
    }
    return 0;
  }

  getVisibleQuantity(internalQuantity: number, productUnitFactor: number): number {
    return ProductUnitHelper.toVisibleQuantity(internalQuantity, productUnitFactor);
  }

  getProductUnitName(item: { ProductUnitName?: string }): string {
    return item?.ProductUnitName || 'NIU';
  }

  async CreateCode(): Promise<string> {
    const rpt: ResponseWsDto = await this.creditNoteService.CreateCode();

    if (rpt.ErrorStatus) {
      this.toastrService.error(rpt.Message);
      throw new Error(rpt.Message);
    }

    return String(rpt.Data);
  }

  setUnit(event: any, item: CreditNoteDetDto) {
    let inputValue = Number(event.target.value);
    const maxUnits = ProductUnitHelper.toVisibleQuantity(
      item.CreditNoteDet.NumUnit,
      item.CreditNoteDet.ProductUnitFactor
    );

    if (inputValue > maxUnits) {
      inputValue = maxUnits;
    } else if (inputValue < 0) {
      inputValue = 0;
    }
    item.CreditNoteDet.NumUnitStockReturned = ProductUnitHelper.toInternalQuantity(
      inputValue,
      item.CreditNoteDet.ProductUnitFactor
    );
    event.target.value = inputValue;
  }

}
