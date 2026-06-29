import { Component, OnInit } from '@angular/core';
import { CreditNoteDetailDto } from '../../model/dto/CreditNoteDetailDto';
import { ActivatedRoute } from '@angular/router';
import { CreditNoteService } from '../../service/CreditNote.service';
import { ToastrService } from 'ngx-toastr';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { TicketSunatService } from '../../service/TicketSunatService';
import { ProductUnitHelper } from 'src/app/enterprise/shared/helper/ProductUnitHelper';

@Component({
  selector: 'app-viewcreditnote',
  templateUrl: './viewcreditnote.component.html',
  styleUrls: ['./viewcreditnote.component.css']
})
export class ViewcreditnoteComponent implements OnInit {

  CreditNoteCod: string = "";
  AutoPrint: string = "N";
  Detail: CreditNoteDetailDto = new CreditNoteDetailDto();

  loading = false;

  constructor(
    private route: ActivatedRoute,
    private creditNoteService: CreditNoteService,
    private toastr: ToastrService,
    private ticketSunatService: TicketSunatService
  ) { }

  ngOnInit(): void {
    this.route.queryParamMap.subscribe(qp => {
      this.CreditNoteCod = qp.get('CreditNoteCod') ?? '';
      this.AutoPrint = qp.get('AutoPrint') ?? 'N';
      if (this.CreditNoteCod) this.findDataForm(this.CreditNoteCod);
    });
  }

  async findDataForm(CreditNoteCod: string) {
    this.loading = true;
    try {
      const rpt: ResponseWsDto = await this.creditNoteService.FindById(CreditNoteCod);
      if (rpt && !rpt.ErrorStatus) {
        this.Detail = rpt.Data as CreditNoteDetailDto;
        if (this.AutoPrint == "Y") {
          this.print();
        }
      } else {
        this.toastr.error(rpt?.Message ?? 'No se pudo obtener la nota de crédito', 'Error');
      }
    } catch (e) {
      this.toastr.error('Ocurrió un error al cargar la nota de crédito', 'Error');
    } finally {
      this.loading = false;
    }
  }

  // Etiquetas amigables para la UI
  typeLabel(t: string | undefined): string {
    if (t === 'T') return 'Total';
    if (t === 'P') return 'Parcial';
    return '-';
    // Si luego agregas más tipos, amplía aquí o usa un Pipe
  }

  statusLabel(s: string | undefined): string {
    // Ajusta según tu catálogo real
    switch (s) {
      case 'P': return 'Pendiente';
      case 'C': return 'Confirmado';
      case 'A': return 'Activo';
      default: return s ?? '-';
    }
  }

  stockReturnedLabel(flag: string | undefined): string {
    if (flag === 'Y') return 'Stock devuelto';
    if (flag === 'N') return 'Sin devolución de stock';
    return '-';
  }

  getVisibleQuantity(internalQuantity: number, productUnitFactor: number): number {
    return ProductUnitHelper.toVisibleQuantity(internalQuantity, productUnitFactor);
  }

  getVisibleUnitPrice(internalUnitPrice: number, productUnitFactor: number): number {
    return ProductUnitHelper.toVisibleUnitPrice(internalUnitPrice, productUnitFactor);
  }

  getProductUnitName(item: { ProductUnitName?: string }): string {
    return item?.ProductUnitName || 'NIU';
  }

  trackByDet = (_: number, it: any) =>
    `${it?.CreditNoteDet?.ProductCod}|${it?.CreditNoteDet?.Variant ?? '0000'}`;

  async print() {
    const dataPrint = await this.creditNoteService.findDataPrint(this.CreditNoteCod);
    this.ticketSunatService.printCreditNote(dataPrint);
  }
}
