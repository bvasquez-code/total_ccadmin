import { Component, OnInit } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { ActionModalConfirmService } from 'src/app/enterprise/shared/interface/ActionModalConfirmService';
import { ActionTableService } from 'src/app/enterprise/shared/interface/ActionTableService';
import { DataTablaGeneticDto } from 'src/app/enterprise/shared/model/dto/DataTablaGeneticDto';
import { ResponsePageSearch } from 'src/app/enterprise/shared/model/dto/ResponsePageSearch';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { SunatSubmissionConstants } from '../../model/SunatSubmissionConstants';
import {
  SunatSubmission,
  SunatSubmissionSearch
} from '../../model/SunatSubmissionModels';
import { SunatSubmissionService } from '../../service/sunat-submission.service';

@Component({
  selector: 'app-listsunatsubmission',
  templateUrl: './listsunatsubmission.component.html',
  styleUrls: ['./listsunatsubmission.component.css']
})
export class ListSunatSubmissionComponent
  implements OnInit, ActionTableService<SunatSubmission>, ActionModalConfirmService {

  readonly constants = SunatSubmissionConstants;
  responsePageSearch = new ResponsePageSearch<SunatSubmission>();
  dataTablaGenetic = new DataTablaGeneticDto<SunatSubmission>();
  selectedSubmission: SunatSubmission | null = null;
  loading = false;
  retrying = false;
  search: SunatSubmissionSearch = {
    Query: '',
    StoreCod: '',
    RequestType: '',
    SendStatus: '',
    DateStart: null,
    DateEnd: null,
    Page: 1
  };

  constructor(
    private sunatSubmissionService: SunatSubmissionService,
    private toastrService: ToastrService
  ) {}

  ngOnInit(): void {
    void this.findAll(1);
  }

  filter(page: number): void {
    void this.findAll(page);
  }

  async findAll(page: number, query: string = ''): Promise<void> {
    if (page <= 0) return;
    this.loading = true;
    this.search.Page = page;
    if (query) this.search.Query = query;
    try {
      const response = await this.sunatSubmissionService.findAll(this.search);
      if (response.ErrorStatus) {
        this.toastrService.error(response.Message);
        return;
      }
      this.responsePageSearch = this.normalizePage(
        response.Data as ResponsePageSearch<SunatSubmission>
      );
      this.loadingTable(this.responsePageSearch);
    } catch (error) {
      this.toastrService.error(this.errorMessage(error));
    } finally {
      this.loading = false;
    }
  }

  loadingTable(responsePageSearch: ResponsePageSearch<SunatSubmission>): void {
    const data = new DataTablaGeneticDto<SunatSubmission>();
    data.init(
      [
        { Name: 'Código', key: 'SunatSubmissionCod' },
        {
          Name: 'Local',
          key: 'StoreCod',
          FunctionKey: (item: SunatSubmission) =>
            `${item.StoreCod} - ${item.StoreName}`
        },
        { Name: 'Documento origen', key: 'SourceDocumentCod' },
        {
          Name: 'Tipo',
          key: 'RequestType',
          FunctionKey: (item: SunatSubmission) =>
            SunatSubmissionConstants.requestTypeDescription(item.RequestType)
        },
        {
          Name: 'Estado envío',
          key: 'SendStatus',
          IsStatus: true,
          Html: {
            P: 'badge badge-sm bgc-warning-d1 text-white pb-1 px-25',
            W: 'badge badge-sm bgc-primary text-white pb-1 px-25',
            S: 'badge badge-sm bgc-success-d1 text-white pb-1 px-25',
            E: 'badge badge-sm bgc-red-d1 text-white pb-1 px-25'
          },
          Mask: {
            P: 'Pendiente',
            W: 'Enviando',
            S: 'Enviado',
            E: 'Error'
          }
        },
        {
          Name: 'Estado SUNAT',
          key: 'SunatStatus',
          FunctionKey: (item: SunatSubmission) =>
            SunatSubmissionConstants.sunatStatusDescription(item.SunatStatus)
        },
        { Name: 'Intentos', key: 'AttemptCount' },
        { Name: 'Último intento', key: 'LastAttemptDate', IsDate: true },
        {
          Name: 'Motivo del error',
          key: 'LastErrorReason',
          FunctionKey: (item: SunatSubmission) => this.shortError(item.LastErrorReason)
        },
        {
          Name: 'Opciones',
          ColumnAction: true,
          Id: ['SunatSubmissionCod'],
          Options: [
            {
              Type: 'Modal',
              Name: 'fa fa-eye',
              Title: 'Ver detalle del envío',
              ID: 'modal_sunat_submission_detail'
            },
            {
              Type: 'Modal',
              Name: 'fa fa-redo',
              Title: 'Reenviar manualmente',
              ID: 'modal_sunat_submission_retry',
              Function: (item: SunatSubmission) => this.canRetry(item)
            }
          ]
        }
      ],
      { data: responsePageSearch },
      'Documentos enviados a SUNAT'
    );
    this.dataTablaGenetic = data;
  }

  getDataRow(item: SunatSubmission): void {
    this.selectedSubmission = item;
  }

  actionModal(modalId: string): void {
    if (modalId === 'modal_sunat_submission_retry') {
      void this.retrySelected();
    }
  }

  canRetry(item: SunatSubmission): boolean {
    if (item.SendStatus === SunatSubmissionConstants.ERROR
      || item.SendStatus === SunatSubmissionConstants.PENDING) {
      return true;
    }
    if (item.SendStatus !== SunatSubmissionConstants.SENDING) return false;
    if (!item.LastAttemptDate) return true;
    const lastAttemptTime = new Date(item.LastAttemptDate).getTime();
    return Number.isFinite(lastAttemptTime)
      && lastAttemptTime <= Date.now()
        - SunatSubmissionConstants.SENDING_RETRY_DELAY_MILLIS;
  }

  retryMessage(): string {
    const document = this.selectedSubmission?.SourceDocumentCod ?? '';
    return `¿Desea reenviar manualmente el documento ${document} a SUNAT?`;
  }

  sendStatusDescription(value: string): string {
    const descriptions: Record<string, string> = {
      P: 'Pendiente',
      W: 'Enviando',
      S: 'Enviado',
      E: 'Error'
    };
    return descriptions[value] ?? value;
  }

  sendStatusBadgeClass(value: string): string {
    const classes: Record<string, string> = {
      P: 'bgc-warning-d1',
      W: 'bgc-primary',
      S: 'bgc-success-d1',
      E: 'bgc-red-d1'
    };
    return classes[value] ?? 'bgc-grey-d1';
  }

  sunatStatusBadgeClass(value: string | null): string {
    if (value === 'ACE') return 'bgc-success-d1';
    if (value === 'OBS' || value === 'PEN' || value === 'RET') {
      return 'bgc-warning-d1';
    }
    if (value === 'REJ' || value === 'ERR' || value === 'ANU') {
      return 'bgc-red-d1';
    }
    return value ? 'bgc-primary' : 'bgc-grey-d1';
  }

  private async retrySelected(): Promise<void> {
    if (!this.selectedSubmission || this.retrying) return;
    this.retrying = true;
    try {
      const response: ResponseWsDto = await this.sunatSubmissionService.retry(
        this.selectedSubmission.SunatSubmissionCod
      );
      if (response.ErrorStatus) {
        this.toastrService.error(response.Message);
        await this.findAll(this.search.Page);
        return;
      }
      const updated = this.normalizeSubmission(response.Data);
      if (updated.SendStatus === SunatSubmissionConstants.SENT) {
        this.toastrService.success('Documento reenviado correctamente a SUNAT');
      } else {
        this.toastrService.warning(
          updated.LastErrorReason || 'SUNAT no confirmó el procesamiento del documento'
        );
      }
      await this.findAll(this.search.Page);
    } catch (error) {
      this.toastrService.error(this.errorMessage(error));
      await this.findAll(this.search.Page);
    } finally {
      this.retrying = false;
    }
  }

  private shortError(value: string | null): string {
    if (!value) return '-';
    return value.length > 90 ? `${value.substring(0, 87)}...` : value;
  }

  private normalizePage(
    response: ResponsePageSearch<SunatSubmission>
  ): ResponsePageSearch<SunatSubmission> {
    response.resultSearch = (response.resultSearch ?? [])
      .map(item => this.normalizeSubmission(item));
    response.StarResult = response.TotalResult === 0 ? 0 : response.StarResult;
    response.EndResult = Math.min(response.EndResult, response.TotalResult);
    return response;
  }

  private normalizeSubmission(source: unknown): SunatSubmission {
    const item = (source ?? {}) as Record<string, unknown>;
    const value = (field: string): unknown => {
      const camelCaseField = `${field.charAt(0).toLowerCase()}${field.substring(1)}`;
      return item[field] ?? item[camelCaseField];
    };
    return {
      SunatSubmissionCod: String(value('SunatSubmissionCod') ?? ''),
      StoreCod: String(value('StoreCod') ?? ''),
      StoreName: String(value('StoreName') ?? ''),
      SourceModule: String(value('SourceModule') ?? ''),
      SourceDocumentCod: String(value('SourceDocumentCod') ?? ''),
      SourceDocumentType: String(value('SourceDocumentType') ?? ''),
      SunatDocumentType: String(value('SunatDocumentType') ?? ''),
      Series: String(value('Series') ?? ''),
      Correlative: Number(value('Correlative') ?? 0),
      RequestType: String(value('RequestType') ?? ''),
      SendStatus: String(value('SendStatus') ?? ''),
      SunatStatus: this.nullableString(value('SunatStatus')),
      RemoteSunatDocumentCod: this.nullableString(value('RemoteSunatDocumentCod')),
      SunatTicket: this.nullableString(value('SunatTicket')),
      AttemptCount: Number(value('AttemptCount') ?? 0),
      LastAttemptDate: this.nullableString(value('LastAttemptDate')),
      LastSuccessDate: this.nullableString(value('LastSuccessDate')),
      LastAttemptUser: this.nullableString(value('LastAttemptUser')),
      LastResponseStatus: this.nullableString(value('LastResponseStatus')),
      LastErrorReason: this.nullableString(value('LastErrorReason')),
      CreationUser: String(value('CreationUser') ?? ''),
      CreationDate: String(value('CreationDate') ?? ''),
      ModifyUser: this.nullableString(value('ModifyUser')),
      ModifyDate: String(value('ModifyDate') ?? '')
    };
  }

  private nullableString(value: unknown): string | null {
    return value === null || value === undefined ? null : String(value);
  }

  private errorMessage(error: unknown): string {
    if (error instanceof Error && error.message) return error.message;
    return 'No se pudo completar la operación SUNAT';
  }
}
