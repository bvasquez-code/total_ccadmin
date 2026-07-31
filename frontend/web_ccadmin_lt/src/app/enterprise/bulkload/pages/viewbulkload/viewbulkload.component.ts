import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { BulkLoadConstants } from '../../model/BulkLoadConstants';
import {
  BulkLoadDetail,
  BulkLoadDestination,
  BulkLoadError,
  BulkLoadRegister,
  PageResponse
} from '../../model/BulkLoadModels';
import { BulkLoadService } from '../../service/bulk-load.service';

@Component({
  selector: 'app-viewbulkload',
  templateUrl: './viewbulkload.component.html',
  styleUrls: ['./viewbulkload.component.css']
})
export class ViewBulkLoadComponent implements OnInit {
  readonly constants = BulkLoadConstants;
  bulkLoadCod = '';
  current: BulkLoadRegister | null = null;
  details: PageResponse<BulkLoadDetail> = this.emptyPage();
  storeCod = '';
  processStatus = '';
  destinationPage = 1;
  loading = false;
  loadingDetails = false;
  readonly destinationPageSize = 6;

  readonly detailStatusList = [
    { Code: '', Name: 'Todos los estados' },
    { Code: BulkLoadConstants.PENDING, Name: 'Pendiente' },
    { Code: BulkLoadConstants.WORKING, Name: 'Procesando' },
    { Code: BulkLoadConstants.CONFIRMED, Name: 'Confirmado' },
    { Code: BulkLoadConstants.ERROR, Name: 'Error' },
    { Code: BulkLoadConstants.CANCELLED, Name: 'Anulado' }
  ];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private service: BulkLoadService,
    private toastr: ToastrService
  ) {}

  ngOnInit(): void {
    this.bulkLoadCod = this.route.snapshot.queryParamMap.get('BulkLoadCod')
      ?? this.route.snapshot.queryParamMap.get('code')
      ?? '';

    if (!this.bulkLoadCod) {
      this.toastr.error('Debe indicar el código de carga masiva');
      void this.goBack();
      return;
    }

    void this.loadBulkLoad(1);
  }

  async refresh(): Promise<void> {
    await this.loadBulkLoad(this.details.Page || 1);
  }

  async loadDetails(page: number): Promise<void> {
    if (!this.current || page < 1) return;
    this.loadingDetails = true;
    try {
      const response = await this.service.findDetails({
        BulkLoadCod: this.bulkLoadCod,
        StoreCod: this.storeCod,
        ProcessStatus: this.processStatus,
        Page: page
      });
      if (response.ErrorStatus) {
        this.toastr.error(response.Message);
        return;
      }
      this.details = response.Data as PageResponse<BulkLoadDetail>;
    } finally {
      this.loadingDetails = false;
    }
  }

  get filteredDestinationList(): BulkLoadDestination[] {
    const destinationList = this.current?.DestinationList ?? [];
    if (!this.storeCod) return destinationList;
    return destinationList.filter(
      destination => destination.StoreCod === this.storeCod
    );
  }

  get pagedDestinationList(): BulkLoadDestination[] {
    const start = (this.destinationPage - 1) * this.destinationPageSize;
    return this.filteredDestinationList.slice(
      start,
      start + this.destinationPageSize
    );
  }

  get destinationTotalPages(): number {
    return Math.max(
      1,
      Math.ceil(this.filteredDestinationList.length / this.destinationPageSize)
    );
  }

  get destinationStartResult(): number {
    if (this.filteredDestinationList.length === 0) return 0;
    return (this.destinationPage - 1) * this.destinationPageSize + 1;
  }

  get destinationEndResult(): number {
    return Math.min(
      this.destinationPage * this.destinationPageSize,
      this.filteredDestinationList.length
    );
  }

  get canEdit(): boolean {
    return this.current !== null
      && BulkLoadConstants.isEditable(this.current.Head);
  }

  onStoreFilterChange(): void {
    this.destinationPage = 1;
  }

  setDestinationPage(page: number): void {
    if (page < 1 || page > this.destinationTotalPages) return;
    this.destinationPage = page;
  }

  progress(value: number | null | undefined): number {
    return Math.min(100, Math.max(0, Number(value ?? 0)));
  }

  progressClass(status: string): string {
    if (status === BulkLoadConstants.ERROR) return 'bgc-red-d1';
    if (status === BulkLoadConstants.FINALIZED
      || status === BulkLoadConstants.CONFIRMED) {
      return 'bgc-success-d1';
    }
    if (status === BulkLoadConstants.CANCELLED) return 'bgc-secondary';
    return 'bgc-primary';
  }

  statusClass(status: string): string {
    const classes: Record<string, string> = {
      D: 'badge badge-sm bgc-secondary text-white pb-1 px-25',
      V: 'badge badge-sm bgc-info-d1 text-white pb-1 px-25',
      P: 'badge badge-sm bgc-warning-d1 text-white pb-1 px-25',
      Q: 'badge badge-sm bgc-info-d1 text-white pb-1 px-25',
      W: 'badge badge-sm bgc-primary text-white pb-1 px-25',
      F: 'badge badge-sm bgc-success-d1 text-white pb-1 px-25',
      E: 'badge badge-sm bgc-red-d1 text-white pb-1 px-25',
      X: 'badge badge-sm bgc-secondary text-white pb-1 px-25',
      C: 'badge badge-sm bgc-success-d1 text-white pb-1 px-25'
    };
    return classes[status]
      ?? 'badge badge-sm bgc-secondary text-white pb-1 px-25';
  }

  detailValue(detail: BulkLoadDetail, key: string): string {
    const value = detail.Payload?.[key];
    return value === null || value === undefined || value === ''
      ? '—'
      : String(value);
  }

  errorMessages(detail: BulkLoadDetail): string {
    return this.messages(detail.ErrorDetail, 'ErrorDetail');
  }

  warningMessages(detail: BulkLoadDetail): string {
    return this.messages(detail.WarningDetail, 'WarningDetail');
  }

  resultDescription(detail: BulkLoadDetail): string {
    const result = detail.ResultData;
    if (!result) return '—';

    if (this.current?.Head.BulkLoadType === BulkLoadConstants.TYPE_PRODUCT_PRICE) {
      const oldPrice = this.resultValue(result, 'OldPrice');
      const newPrice = this.resultValue(result, 'NewPrice');
      const changed = result['Changed'] === true ? 'Actualizado' : 'Sin cambio';
      return `${changed}: ${oldPrice} → ${newPrice}`;
    }

    if (this.current?.Head.BulkLoadType === BulkLoadConstants.TYPE_STOCK_ENTRY) {
      const stockEntryCod = this.resultValue(result, 'StockEntryCod');
      const itemNumber = this.resultValue(result, 'ItemNumber');
      return `${stockEntryCod} / ítem ${itemNumber}`;
    }

    return Object.entries(result)
      .map(([key, value]) => `${key}: ${String(value)}`)
      .join(' | ');
  }

  async goBack(): Promise<void> {
    await this.router.navigate(['/enterprise/bulkload/pages/listbulkload']);
  }

  async goEdit(): Promise<void> {
    if (!this.canEdit) return;
    await this.router.navigate(
      ['/enterprise/bulkload/pages/createbulkload'],
      { queryParams: { BulkLoadCod: this.bulkLoadCod } }
    );
  }

  private async loadBulkLoad(detailPage: number): Promise<void> {
    this.loading = true;
    try {
      const response = await this.service.findById(this.bulkLoadCod);
      if (response.ErrorStatus) {
        this.toastr.error(response.Message);
        await this.goBack();
        return;
      }
      this.current = response.Data as BulkLoadRegister;
      if (this.storeCod && !this.current.DestinationList.some(
        destination => destination.StoreCod === this.storeCod
      )) {
        this.storeCod = '';
      }
      this.destinationPage = Math.min(
        this.destinationPage,
        this.destinationTotalPages
      );
      await this.loadDetails(detailPage);
    } finally {
      this.loading = false;
    }
  }

  private messages(
    messages: BulkLoadError[] | undefined,
    preferredField: keyof BulkLoadError
  ): string {
    return (messages ?? [])
      .map(message => String(message[preferredField] ?? message.ErrorDetail ?? ''))
      .filter(message => Boolean(message))
      .join(' | ');
  }

  private resultValue(result: Record<string, unknown>, key: string): string {
    const value = result[key];
    return value === null || value === undefined || value === ''
      ? '—'
      : String(value);
  }

  private emptyPage(): PageResponse<BulkLoadDetail> {
    return {
      resultSearch: [],
      TotalPages: 0,
      TotalResult: 0,
      StarResult: 0,
      EndResult: 0,
      Page: 1
    };
  }
}
