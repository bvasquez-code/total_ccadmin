import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import * as XLSX from 'xlsx';
import { BulkLoadConstants } from '../../model/BulkLoadConstants';
import {
  BulkLoadDetail,
  BulkLoadError,
  BulkLoadParsedRequest,
  BulkLoadRegister,
  BulkLoadSourceRow,
  BulkLoadStoreRow,
  PageResponse
} from '../../model/BulkLoadModels';
import { BulkLoadService } from '../../service/bulk-load.service';

interface FormatDefinition {
  productSheet: string;
  headers: string[];
  types: string[];
  labels: string[];
  valueField: string;
}

@Component({
  selector: 'app-createbulkload',
  templateUrl: './createbulkload.component.html',
  styleUrls: ['./createbulkload.component.css']
})
export class CreateBulkLoadComponent implements OnInit {
  readonly constants = BulkLoadConstants;
  type = BulkLoadConstants.TYPE_PRODUCT_PRICE;
  current: BulkLoadRegister | null = null;
  details: PageResponse<BulkLoadDetail> = this.emptyPage();
  localErrors: BulkLoadError[] = [];
  backendErrors: BulkLoadError[] = [];
  loading = false;
  loadingDetails = false;
  selectedFileName = '';
  private sourceWorkbook: XLSX.WorkBook | null = null;
  private readonly storeSheet = 'LOCALES';

  constructor(
    private service: BulkLoadService,
    private toastr: ToastrService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    const code = this.route.snapshot.queryParamMap.get('code');
    if (code) void this.loadExisting(code);
  }

  get readOnly(): boolean {
    return this.current !== null;
  }

  get errors(): BulkLoadError[] {
    return [...this.localErrors, ...this.backendErrors];
  }

  get canConfirm(): boolean {
    return this.current?.Head.ProcessStatus === BulkLoadConstants.PENDING
      && (this.current.Head.NumErrorDetails ?? 0) === 0;
  }

  downloadTemplate(): void {
    const definition = this.definition();
    const workbook = XLSX.utils.book_new();
    const productSheet = XLSX.utils.aoa_to_sheet([
      definition.headers,
      definition.types,
      definition.labels
    ]);
    productSheet['!cols'] = [{ wch: 24 }, { wch: 24 }];
    const stores = XLSX.utils.aoa_to_sheet([
      ['StoreCod'],
      ['TEXTO(4)'],
      ['CODIGO DE TIENDA']
    ]);
    stores['!cols'] = [{ wch: 24 }];
    XLSX.utils.book_append_sheet(workbook, productSheet, definition.productSheet);
    XLSX.utils.book_append_sheet(workbook, stores, this.storeSheet);
    const fileName = this.type === BulkLoadConstants.TYPE_PRODUCT_PRICE
      ? 'FORMATO_CARGA_DE_PRECIOS.xlsx'
      : 'FORMATO_CARGA_DE_STOCK.xlsx';
    XLSX.writeFile(workbook, fileName);
  }

  async onFileSelected(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.item(0);
    input.value = '';
    if (!file) return;
    if (!file.name.toLowerCase().endsWith('.xlsx')) {
      this.toastr.error('Solo se permiten archivos .xlsx');
      return;
    }

    this.resetResult();
    this.selectedFileName = file.name;
    this.loading = true;
    try {
      const buffer = await file.arrayBuffer();
      const workbook = XLSX.read(buffer, { type: 'array' });
      this.sourceWorkbook = workbook;
      const request = this.parseWorkbook(workbook, file.name);
      if (this.localErrors.length > 0 || request === null) {
        this.toastr.warning(
          `El archivo contiene ${this.localErrors.length} error(es) de formato`
        );
        return;
      }
      const response = await this.service.saveParsed(request);
      if (response.ErrorStatus) {
        this.toastr.error(response.Message);
        return;
      }
      this.current = response.Data as BulkLoadRegister;
      this.backendErrors = this.current.ErrorList ?? [];
      this.type = this.current.Head.BulkLoadType;
      await this.loadDetails(1);
      if (this.backendErrors.length > 0) {
        this.toastr.warning(
          `Se encontraron ${this.backendErrors.length} error(es) de negocio`
        );
      } else {
        this.toastr.success('Archivo validado. Revise el preview antes de confirmar');
      }
    } catch (error) {
      this.toastr.error(this.errorMessage(error));
    } finally {
      this.loading = false;
    }
  }

  async loadDetails(page: number): Promise<void> {
    if (!this.current) return;
    this.loadingDetails = true;
    try {
      const response = await this.service.findDetails({
        BulkLoadCod: this.current.Head.BulkLoadCod,
        StoreCod: '',
        ProcessStatus: '',
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

  async confirm(): Promise<void> {
    if (!this.current || !this.canConfirm) return;
    if (!window.confirm(
      `Se procesarán ${this.current.Head.NumTotalDetails} registros en segundo plano. ¿Desea continuar?`
    )) return;
    this.loading = true;
    try {
      const response = await this.service.confirm(this.current.Head.BulkLoadCod);
      if (response.ErrorStatus) {
        this.toastr.error(response.Message);
        return;
      }
      this.toastr.success('Carga enviada a la cola de procesamiento');
      await this.router.navigate(['/enterprise/bulkload/pages/listbulkload']);
    } finally {
      this.loading = false;
    }
  }

  async cancel(): Promise<void> {
    if (!this.current
      || this.current.Head.ProcessStatus !== BulkLoadConstants.PENDING
      || !window.confirm('¿Desea anular esta carga pendiente?')) return;
    this.loading = true;
    try {
      const response = await this.service.cancel(this.current.Head.BulkLoadCod);
      if (response.ErrorStatus) {
        this.toastr.error(response.Message);
        return;
      }
      this.current = response.Data as BulkLoadRegister;
      this.toastr.success('Carga anulada');
    } finally {
      this.loading = false;
    }
  }

  exportErrors(): void {
    if (this.errors.length === 0) return;
    let workbook = XLSX.utils.book_new();
    if (this.sourceWorkbook) {
      const copy = XLSX.write(this.sourceWorkbook, { type: 'array', bookType: 'xlsx' });
      workbook = XLSX.read(copy, { type: 'array' });
    }
    if (workbook.SheetNames.includes('ERROR_DETAIL')) {
      delete workbook.Sheets['ERROR_DETAIL'];
      workbook.SheetNames = workbook.SheetNames.filter(name => name !== 'ERROR_DETAIL');
    }
    const rows: unknown[][] = [[
      'HOJA', 'FILA', 'LOCAL', 'CAMPO', 'VALOR',
      'CODIGO_ERROR', 'DETALLE_ERROR', 'ADVERTENCIA'
    ]];
    this.errors.forEach(error => rows.push([
      error.Sheet,
      error.RowNumber,
      error.StoreCod ?? '',
      error.Field,
      error.Value ?? '',
      error.ErrorCode,
      error.ErrorDetail,
      error.WarningDetail ?? ''
    ]));
    const sheet = XLSX.utils.aoa_to_sheet(rows);
    sheet['!cols'] = [
      { wch: 22 }, { wch: 10 }, { wch: 10 }, { wch: 24 },
      { wch: 24 }, { wch: 30 }, { wch: 70 }, { wch: 50 }
    ];
    XLSX.utils.book_append_sheet(workbook, sheet, 'ERROR_DETAIL');
    const code = this.current?.Head.BulkLoadCod ?? 'FORMATO';
    XLSX.writeFile(workbook, `ERRORES_${code}.xlsx`);
  }

  detailValue(detail: BulkLoadDetail, key: string): string {
    const value = detail.Payload?.[key];
    return value === null || value === undefined ? '' : String(value);
  }

  detailErrors(detail: BulkLoadDetail): string {
    return (detail.ErrorDetail ?? [])
      .map(error => error.ErrorDetail)
      .filter(message => Boolean(message))
      .join(' | ');
  }

  private async loadExisting(code: string): Promise<void> {
    this.loading = true;
    try {
      const response = await this.service.findById(code);
      if (response.ErrorStatus) {
        this.toastr.error(response.Message);
        await this.router.navigate(['/enterprise/bulkload/pages/listbulkload']);
        return;
      }
      this.current = response.Data as BulkLoadRegister;
      this.type = this.current.Head.BulkLoadType;
      this.selectedFileName = this.current.Head.OriginalFileName;
      await this.loadDetails(1);
    } finally {
      this.loading = false;
    }
  }

  private parseWorkbook(
    workbook: XLSX.WorkBook,
    originalFileName: string
  ): BulkLoadParsedRequest | null {
    const definition = this.definition();
    const productRows = this.readRequiredSheet(workbook, definition.productSheet);
    const storeRows = this.readRequiredSheet(workbook, this.storeSheet);
    if (productRows === null || storeRows === null) return null;

    this.validateMetadata(
      productRows, definition.productSheet,
      definition.headers, definition.types, definition.labels
    );
    this.validateMetadata(
      storeRows, this.storeSheet,
      ['StoreCod'], ['TEXTO(4)'], ['CODIGO DE TIENDA']
    );
    if (this.localErrors.length > 0) return null;

    const rows = this.parseProductRows(productRows, definition);
    const stores = this.parseStoreRows(storeRows);
    this.validateDuplicates(rows, stores);
    this.validateStoreRules(stores);
    if (rows.length === 0) {
      this.addLocalError(definition.productSheet, 0, '', 'ProductCod', '',
        'ROW_REQUIRED', 'Debe registrar al menos un producto');
    }
    if (stores.length === 0) {
      this.addLocalError(this.storeSheet, 0, '', 'StoreCod', '',
        'STORE_REQUIRED', 'Debe registrar al menos un local');
    }
    if (this.localErrors.length > 0) return null;

    return {
      BulkLoadType: this.type,
      SchemaVersion: 1,
      OriginalFileName: originalFileName,
      RowList: rows,
      StoreList: stores
    };
  }

  private readRequiredSheet(
    workbook: XLSX.WorkBook,
    sheetName: string
  ): unknown[][] | null {
    const sheet = workbook.Sheets[sheetName];
    if (!sheet) {
      this.addLocalError(sheetName, 0, '', '', '', 'SHEET_REQUIRED',
        `No existe la hoja ${sheetName}`);
      return null;
    }
    return XLSX.utils.sheet_to_json(sheet, {
      header: 1,
      raw: true,
      defval: ''
    }) as unknown[][];
  }

  private validateMetadata(
    rows: unknown[][],
    sheetName: string,
    headers: string[],
    types: string[],
    labels: string[]
  ): void {
    const expected = [headers, types, labels];
    expected.forEach((expectedRow, rowIndex) => {
      expectedRow.forEach((expectedValue, columnIndex) => {
        const actual = this.text(rows[rowIndex]?.[columnIndex]);
        if (actual !== expectedValue) {
          this.addLocalError(
            sheetName, rowIndex + 1, '', `Columna ${columnIndex + 1}`, actual,
            'FORMAT_METADATA',
            `Se esperaba "${expectedValue}" en la fila ${rowIndex + 1}, columna ${columnIndex + 1}`
          );
        }
      });
    });
  }

  private parseProductRows(
    rows: unknown[][],
    definition: FormatDefinition
  ): BulkLoadSourceRow[] {
    const result: BulkLoadSourceRow[] = [];
    rows.slice(3).forEach((row, index) => {
      const rowNumber = index + 4;
      const productCod = this.text(row[0]);
      const rawValue = row[1];
      if (productCod === '' && this.text(rawValue) === '') return;
      if (productCod === '') {
        this.addLocalError(definition.productSheet, rowNumber, '',
          'ProductCod', '', 'PRODUCT_REQUIRED', 'El código de producto es obligatorio');
      } else if (productCod.length > 20) {
        this.addLocalError(definition.productSheet, rowNumber, '',
          'ProductCod', productCod, 'PRODUCT_LENGTH',
          'El código de producto admite hasta 20 caracteres');
      }

      const numeric = this.numeric(rawValue);
      if (numeric === null) {
        this.addLocalError(definition.productSheet, rowNumber, '',
          definition.valueField, this.text(rawValue), 'NUMBER_FORMAT',
          'El valor debe ser numérico');
      } else if (this.type === BulkLoadConstants.TYPE_PRODUCT_PRICE) {
        const decimalPlaces = this.decimalPlaces(rawValue);
        if (numeric <= 0 || numeric > 99999999999999.99 || decimalPlaces > 2) {
          this.addLocalError(definition.productSheet, rowNumber, '',
            definition.valueField, this.text(rawValue), 'PRICE_FORMAT',
            'El precio debe cumplir NUMERO(16,2) y ser mayor a cero');
        }
      } else if (!Number.isInteger(numeric) || numeric < 1 || numeric > 9999999) {
        this.addLocalError(definition.productSheet, rowNumber, '',
          definition.valueField, this.text(rawValue), 'STOCK_FORMAT',
          'La cantidad debe cumplir NUMERO(7): entero entre 1 y 9999999');
      }
      result.push({
        RowNumber: rowNumber,
        ProductCod: productCod,
        Value: numeric === null ? this.text(rawValue) : String(numeric)
      });
    });
    return result;
  }

  private parseStoreRows(rows: unknown[][]): BulkLoadStoreRow[] {
    const result: BulkLoadStoreRow[] = [];
    rows.slice(3).forEach((row, index) => {
      const rowNumber = index + 4;
      const storeCod = this.text(row[0]).toUpperCase();
      if (storeCod === '') return;
      if (storeCod !== 'TODOS' && storeCod.length > 4) {
        this.addLocalError(this.storeSheet, rowNumber, storeCod,
          'StoreCod', storeCod, 'STORE_LENGTH',
          'El código de local admite hasta 4 caracteres');
      }
      result.push({ RowNumber: rowNumber, StoreCod: storeCod });
    });
    return result;
  }

  private validateDuplicates(
    rows: BulkLoadSourceRow[],
    stores: BulkLoadStoreRow[]
  ): void {
    const products = new Set<string>();
    rows.forEach(row => {
      if (products.has(row.ProductCod)) {
        this.addLocalError(this.definition().productSheet, row.RowNumber, '',
          'ProductCod', row.ProductCod, 'PRODUCT_DUPLICATED',
          'El producto está repetido');
      }
      products.add(row.ProductCod);
    });
    const storeCodes = new Set<string>();
    stores.forEach(store => {
      if (storeCodes.has(store.StoreCod)) {
        this.addLocalError(this.storeSheet, store.RowNumber, store.StoreCod,
          'StoreCod', store.StoreCod, 'STORE_DUPLICATED',
          'El local está repetido');
      }
      storeCodes.add(store.StoreCod);
    });
  }

  private validateStoreRules(stores: BulkLoadStoreRow[]): void {
    const hasAll = stores.some(store => store.StoreCod === 'TODOS');
    if (this.type === BulkLoadConstants.TYPE_STOCK_ENTRY) {
      if (hasAll) {
        const item = stores.find(store => store.StoreCod === 'TODOS');
        this.addLocalError(this.storeSheet, item?.RowNumber ?? 0, 'TODOS',
          'StoreCod', 'TODOS', 'STORE_WILDCARD_NOT_ALLOWED',
          'La carga de stock no permite el comodín TODOS');
      }
      if (stores.length !== 1) {
        this.addLocalError(this.storeSheet, 0, '', 'StoreCod',
          stores.map(store => store.StoreCod).join(', '), 'STOCK_SINGLE_STORE',
          'La carga de stock requiere exactamente un local');
      }
    } else if (hasAll && stores.length !== 1) {
      const item = stores.find(store => store.StoreCod === 'TODOS');
      this.addLocalError(this.storeSheet, item?.RowNumber ?? 0, 'TODOS',
        'StoreCod', 'TODOS', 'STORE_WILDCARD_EXCLUSIVE',
        'TODOS debe ser el único registro de la hoja LOCALES');
    }
  }

  private definition(): FormatDefinition {
    if (this.type === BulkLoadConstants.TYPE_STOCK_ENTRY) {
      return {
        productSheet: 'PRODUCTO_STOCK',
        headers: ['ProductCod', 'NumPhysicalStock'],
        types: ['TEXTO(20)', 'NUMERO(7)'],
        labels: ['CODIGO DE PRODUCTO', 'STOCK'],
        valueField: 'NumPhysicalStock'
      };
    }
    return {
      productSheet: 'PRODUCTO_PRECIO',
      headers: ['ProductCod', 'NumPrice'],
      types: ['TEXTO(20)', 'NUMERO(16,2)'],
      labels: ['CODIGO DE PRODUCTO', 'PRECIO DE PRODUCTO'],
      valueField: 'NumPrice'
    };
  }

  private numeric(value: unknown): number | null {
    if (typeof value === 'number') return Number.isFinite(value) ? value : null;
    const text = this.text(value).replace(',', '.');
    if (!/^[+-]?\d+(?:\.\d+)?$/.test(text)) return null;
    const result = Number(text);
    return Number.isFinite(result) ? result : null;
  }

  private decimalPlaces(value: unknown): number {
    const text = this.text(value).replace(',', '.');
    if (text.includes('.')) return text.split('.')[1]?.length ?? 0;
    if (typeof value === 'number') {
      const numberText = value.toString();
      return numberText.includes('.') ? numberText.split('.')[1]?.length ?? 0 : 0;
    }
    return 0;
  }

  private addLocalError(
    sheet: string,
    row: number,
    storeCod: string,
    field: string,
    value: string,
    code: string,
    detail: string
  ): void {
    this.localErrors.push({
      Sheet: sheet,
      RowNumber: row,
      StoreCod: storeCod,
      Field: field,
      Value: value,
      ErrorCode: code,
      ErrorDetail: detail
    });
  }

  private resetResult(): void {
    this.current = null;
    this.details = this.emptyPage();
    this.localErrors = [];
    this.backendErrors = [];
    this.sourceWorkbook = null;
  }

  private text(value: unknown): string {
    return value === null || value === undefined ? '' : String(value).trim();
  }

  private errorMessage(error: unknown): string {
    if (error instanceof Error) return error.message;
    return 'No se pudo leer o validar el archivo Excel';
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
