import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import * as XLSX from 'xlsx';
import { AlertService } from 'src/app/enterprise/shared/service/AlertService';
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
  valueField?: string;
  templateFileName: string;
  requiresDestinations: boolean;
  useFirstSheet?: boolean;
}

@Component({
  selector: 'app-createbulkload',
  templateUrl: './createbulkload.component.html',
  styleUrls: ['./createbulkload.component.css']
})
export class CreateBulkLoadComponent implements OnInit {
  readonly constants = BulkLoadConstants;
  type = BulkLoadConstants.TYPE_PRODUCT_PRICE;
  typeLocked = false;
  returnUrl = '/enterprise/bulkload/pages/listbulkload';
  isEditMode = false;
  current: BulkLoadRegister | null = null;
  details: PageResponse<BulkLoadDetail> = this.emptyPage();
  localErrors: BulkLoadError[] = [];
  backendErrors: BulkLoadError[] = [];
  loading = false;
  loadingDetails = false;
  selectedFileName = '';
  detailFilter = {
    Query: '',
    StoreCod: '',
    ProcessStatus: ''
  };
  readonly detailStatusList = [
    { Code: '', Name: 'Todos los estados' },
    { Code: BulkLoadConstants.ERROR, Name: 'Sólo errores' },
    { Code: BulkLoadConstants.PENDING, Name: 'Pendiente' },
    { Code: BulkLoadConstants.WORKING, Name: 'Procesando' },
    { Code: BulkLoadConstants.CONFIRMED, Name: 'Confirmado' },
    { Code: BulkLoadConstants.CANCELLED, Name: 'Anulado' }
  ];
  private sourceWorkbook: XLSX.WorkBook | null = null;
  private readonly storeSheet = 'LOCALES';

  constructor(
    private service: BulkLoadService,
    private toastr: ToastrService,
    private alertService: AlertService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    const routeType = this.route.snapshot.data['BulkLoadType'] as string | undefined;
    const routeReturnUrl = this.route.snapshot.data['ReturnUrl'] as string | undefined;
    if (routeType) {
      this.type = routeType;
      this.typeLocked = true;
    }
    if (routeReturnUrl) {
      this.returnUrl = routeReturnUrl.startsWith('/')
        ? routeReturnUrl : `/${routeReturnUrl}`;
    }

    const bulkLoadCod = this.route.snapshot.queryParamMap.get('BulkLoadCod')
      ?? this.route.snapshot.queryParamMap.get('code')
      ?? '';
    if (!bulkLoadCod) return;

    this.isEditMode = true;
    void this.loadExisting(bulkLoadCod);
  }

  get errors(): BulkLoadError[] {
    return [...this.localErrors, ...this.backendErrors];
  }

  get canConfirm(): boolean {
    return this.current?.Head.ProcessStatus === BulkLoadConstants.PENDING
      && (this.current.Head.NumErrorDetails ?? 0) === 0;
  }

  get canCorrect(): boolean {
    return this.current !== null
      && this.isEditMode
      && BulkLoadConstants.isCorrectableError(this.current.Head);
  }

  get canRetry(): boolean {
    return this.current?.Head.ProcessStatus === BulkLoadConstants.ERROR
      && !this.canCorrect;
  }

  downloadTemplate(): void {
    if (BulkLoadConstants.USE_LEGACY_GENERATED_TEMPLATES) {
      this.downloadGeneratedTemplate();
      return;
    }

    const fileName = this.templateFileName();
    const link = document.createElement('a');
    link.href = `${BulkLoadConstants.TEMPLATE_ASSET_PATH}/${fileName}`;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  private templateFileName(): string {
    return this.definition().templateFileName;
  }

  private downloadGeneratedTemplate(): void {
    const definition = this.definition();
    const workbook = XLSX.utils.book_new();
    const productSheet = XLSX.utils.aoa_to_sheet([
      definition.headers,
      definition.types,
      definition.labels
    ]);
    productSheet['!cols'] = [{ wch: 24 }, { wch: 24 }];
    XLSX.utils.book_append_sheet(workbook, productSheet, definition.productSheet);
    if (definition.requiresDestinations) {
      const stores = XLSX.utils.aoa_to_sheet([
        ['StoreCod'],
        ['TEXTO(4)'],
        ['CODIGO DE TIENDA']
      ]);
      stores['!cols'] = [{ wch: 24 }];
      XLSX.utils.book_append_sheet(workbook, stores, this.storeSheet);
    }
    XLSX.writeFile(workbook, this.templateFileName());
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

    const correctionCode = this.canCorrect
      ? this.current?.Head.BulkLoadCod ?? ''
      : '';
    this.resetResult(Boolean(correctionCode));
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
      const response = correctionCode
        ? await this.service.correctParsed(correctionCode, request)
        : await this.service.saveParsed(request);
      if (response.ErrorStatus) {
        this.toastr.error(response.Message);
        return;
      }
      this.current = response.Data as BulkLoadRegister;
      this.backendErrors = this.current.ErrorList ?? [];
      this.type = this.current.Head.BulkLoadType;
      this.resetDetailFilterForCurrent();
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
        Query: this.detailFilter.Query,
        StoreCod: this.detailFilter.StoreCod,
        ProcessStatus: this.detailFilter.ProcessStatus,
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

  filterDetails(): void {
    void this.loadDetails(1);
  }

  clearDetailFilters(): void {
    this.detailFilter = {
      Query: '',
      StoreCod: '',
      ProcessStatus: ''
    };
    void this.loadDetails(1);
  }

  async openManualProductCreation(): Promise<void> {
    const routePath = this.route.snapshot.routeConfig?.path
      ?? 'enterprise/bulkload/pages/createbulkload';
    await this.router.navigate(
      ['/enterprise/bulkload/pages/createbulkloadproducts'],
      { queryParams: { returnTo: `/${routePath}` } }
    );
  }

  async confirm(): Promise<void> {
    if (!this.current || !this.canConfirm) return;
    const confirmation = await this.alertService.waring(
      `Se procesarán ${this.current.Head.NumTotalDetails} registros en segundo plano. `
        + 'Podrá revisar el avance desde la bandeja de cargas masivas.',
      'Confirmar carga masiva'
    );
    if (!confirmation.isConfirmed) return;

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
      || this.current.Head.ProcessStatus !== BulkLoadConstants.PENDING) return;
    const confirmation = await this.alertService.waring(
      'La carga pendiente será anulada y ya no podrá enviarse a procesamiento.',
      'Anular carga masiva'
    );
    if (!confirmation.isConfirmed) return;

    this.loading = true;
    try {
      const response = await this.service.cancel(this.current.Head.BulkLoadCod);
      if (response.ErrorStatus) {
        this.toastr.error(response.Message);
        return;
      }
      this.current = response.Data as BulkLoadRegister;
      this.toastr.success('Carga anulada');
      await this.router.navigate(['/enterprise/bulkload/pages/listbulkload']);
    } finally {
      this.loading = false;
    }
  }

  async retry(): Promise<void> {
    if (!this.current || !this.canRetry) return;
    const confirmation = await this.alertService.waring(
      'Se volverán a procesar únicamente los detalles pendientes. '
        + 'Los bloques ya confirmados no se repetirán.',
      'Reintentar carga masiva'
    );
    if (!confirmation.isConfirmed) return;

    this.loading = true;
    try {
      const response = await this.service.retry(this.current.Head.BulkLoadCod);
      if (response.ErrorStatus) {
        this.toastr.error(response.Message);
        return;
      }
      this.toastr.success('Reintento enviado a la cola de procesamiento');
      await this.router.navigate(['/enterprise/bulkload/pages/listbulkload']);
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

  private async loadExisting(bulkLoadCod: string): Promise<void> {
    this.loading = true;
    try {
      const response = await this.service.findById(bulkLoadCod);
      if (response.ErrorStatus) {
        this.toastr.error(response.Message);
        await this.router.navigate(['/enterprise/bulkload/pages/listbulkload']);
        return;
      }

      const register = response.Data as BulkLoadRegister;
      if (!BulkLoadConstants.isEditable(register.Head)) {
        this.toastr.warning(
          'Sólo se pueden editar cargas pendientes o con errores de validación'
        );
        await this.router.navigate(
          ['/enterprise/bulkload/pages/viewbulkload'],
          { queryParams: { BulkLoadCod: register.Head.BulkLoadCod } }
        );
        return;
      }

      this.current = register;
      this.type = register.Head.BulkLoadType;
      this.selectedFileName = register.Head.OriginalFileName;
      this.backendErrors = register.ErrorList ?? [];
      this.resetDetailFilterForCurrent();
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
    const sourceSheet = definition.useFirstSheet
      ? workbook.SheetNames[0] ?? definition.productSheet
      : definition.productSheet;
    const productRows = this.readRequiredSheet(workbook, sourceSheet);
    if (productRows === null) return null;

    let rows: BulkLoadSourceRow[];
    if (definition.requiresDestinations) {
      const storeRows = this.readRequiredSheet(workbook, this.storeSheet);
      if (storeRows === null) return null;
      this.validateMetadata(
        productRows, sourceSheet,
        definition.headers, definition.types, definition.labels
      );
      this.validateMetadata(
        storeRows, this.storeSheet,
        ['StoreCod'], ['TEXTO(4)'], ['CODIGO DE TIENDA']
      );
      if (this.localErrors.length > 0) return null;
      rows = this.parseProductRows(productRows, definition);
      const stores = this.parseStoreRows(storeRows);
      this.validateDuplicates(rows, stores);
      this.validateStoreRules(stores);
      if (stores.length === 0) {
        this.addLocalError(this.storeSheet, 0, '', 'StoreCod', '',
          'STORE_REQUIRED', 'Debe registrar al menos un local');
      }
      if (rows.length === 0) {
        this.addLocalError(sourceSheet, 0, '', 'ProductCod', '',
          'ROW_REQUIRED', 'Debe registrar al menos un producto');
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

    this.validateFlexibleMetadata(productRows, sourceSheet, definition);
    if (this.localErrors.length > 0) return null;
    rows = this.parseGenericRows(productRows, sourceSheet, definition);
    if (rows.length === 0) {
      this.addLocalError(sourceSheet, 0, '', 'Row', '',
        'ROW_REQUIRED', 'Debe registrar al menos una fila');
    }
    if (this.localErrors.length > 0) return null;

    return {
      BulkLoadType: this.type,
      SchemaVersion: 1,
      OriginalFileName: originalFileName,
      RowList: rows,
      StoreList: []
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

  private validateFlexibleMetadata(
    rows: unknown[][],
    sheetName: string,
    definition: FormatDefinition
  ): void {
    if (rows.length < 3) {
      this.addLocalError(sheetName, 0, '', '', '', 'FORMAT_METADATA',
        'El formato debe contener las filas de columnas, tipos y etiquetas');
      return;
    }
    const actualHeaders = (rows[0] ?? []).map(value => this.text(value));
    const repeatedHeaders = actualHeaders.filter(
      (header, index) => header !== '' && actualHeaders.indexOf(header) !== index
    );
    repeatedHeaders.forEach(header => this.addLocalError(
      sheetName, 1, '', header, header, 'FORMAT_HEADER_DUPLICATED',
      `La columna "${header}" está repetida`
    ));

    definition.headers.forEach(header => {
      const columnIndex = actualHeaders.indexOf(header);
      if (columnIndex < 0) {
        this.addLocalError(sheetName, 1, '', header, '',
          'FORMAT_HEADER_REQUIRED', `Falta la columna "${header}"`);
        return;
      }
      const typeRule = this.text(rows[1]?.[columnIndex]).toUpperCase();
      if (!/^TEXTO(?:\(\d+\))?$/.test(typeRule)
        && !/^NUMERO(?:\(\d+(?:,\d+)?\))?$/.test(typeRule)) {
        this.addLocalError(sheetName, 2, '', header, typeRule,
          'FORMAT_TYPE_INVALID',
          `El tipo de la columna "${header}" no está soportado`);
      }
      if (this.text(rows[2]?.[columnIndex]) === '') {
        this.addLocalError(sheetName, 3, '', header, '',
          'FORMAT_LABEL_REQUIRED',
          `Falta la etiqueta de la columna "${header}"`);
      }
    });
  }

  private parseGenericRows(
    rows: unknown[][],
    sheetName: string,
    definition: FormatDefinition
  ): BulkLoadSourceRow[] {
    const result: BulkLoadSourceRow[] = [];
    const headers = (rows[0] ?? []).map(value => this.text(value));
    const types = (rows[1] ?? []).map(value => this.text(value).toUpperCase());
    const keyField = this.type === BulkLoadConstants.TYPE_BRAND_CREATE
      ? 'BrandCod'
      : this.type === BulkLoadConstants.TYPE_CATEGORY_CREATE
        ? 'CategoryCod' : 'ProductCod';
    const seenKeys = new Set<string>();

    rows.slice(3).forEach((row, index) => {
      const rowNumber = index + 4;
      const hasData = definition.headers.some(header => {
        const columnIndex = headers.indexOf(header);
        return columnIndex >= 0 && this.text(row[columnIndex]) !== '';
      });
      if (!hasData) return;

      const payload: Record<string, unknown> = {};
      definition.headers.forEach(header => {
        const columnIndex = headers.indexOf(header);
        const rawValue = columnIndex < 0 ? '' : row[columnIndex];
        const typeRule = columnIndex < 0 ? '' : types[columnIndex];
        let parsedValue: unknown = this.text(rawValue);
        const textMatch = typeRule.match(/^TEXTO(?:\((\d+)\))?$/);
        const numberMatch = typeRule.match(/^NUMERO(?:\((\d+)(?:,(\d+))?\))?$/);

        if (textMatch) {
          const maxLength = Number(textMatch[1] ?? 0);
          if (maxLength > 0 && this.text(rawValue).length > maxLength) {
            this.addLocalError(
              sheetName, rowNumber, '', header, this.text(rawValue),
              'TEXT_LENGTH',
              `${header} admite hasta ${maxLength} caracteres`
            );
          }
        } else if (numberMatch) {
          if (this.text(rawValue) === '') {
            parsedValue = 0;
          } else {
            const numericValue = this.numeric(rawValue);
            if (numericValue === null) {
              this.addLocalError(
                sheetName, rowNumber, '', header, this.text(rawValue),
                'NUMBER_FORMAT', `${header} debe ser numérico`
              );
            } else {
              parsedValue = numericValue;
              const scale = Number(numberMatch[2] ?? 0);
              if (numberMatch[2] && this.decimalPlaces(rawValue) > scale) {
                this.addLocalError(
                  sheetName, rowNumber, '', header, this.text(rawValue),
                  'NUMBER_SCALE',
                  `${header} admite hasta ${scale} decimales`
                );
              }
            }
          }
        }
        payload[header] = parsedValue;
      });

      this.validateGenericRequiredFields(payload, sheetName, rowNumber);
      const businessKey = this.text(payload[keyField]);
      const normalizedKey = businessKey.toUpperCase();
      if (normalizedKey !== '' && seenKeys.has(normalizedKey)) {
        this.addLocalError(
          sheetName, rowNumber, '', keyField, businessKey,
          'BUSINESS_KEY_DUPLICATED',
          `El código "${businessKey}" está repetido en el archivo`
        );
      }
      seenKeys.add(normalizedKey);
      result.push({
        RowNumber: rowNumber,
        BusinessKey: businessKey,
        Payload: payload,
        ProductCod: keyField === 'ProductCod' ? businessKey : undefined
      });
    });
    return result;
  }

  private validateGenericRequiredFields(
    payload: Record<string, unknown>,
    sheetName: string,
    rowNumber: number
  ): void {
    const requiredFields = this.type === BulkLoadConstants.TYPE_PRODUCT_CREATE
      ? ['ProductCod', 'ProductName', 'BrandCod', 'CategoryCod']
      : this.type === BulkLoadConstants.TYPE_BRAND_CREATE
        ? ['BrandCod', 'BrandName']
        : ['CategoryCod', 'CategoryName'];
    requiredFields.forEach(field => {
      if (this.text(payload[field]) === '') {
        this.addLocalError(
          sheetName, rowNumber, '', field, '', 'FIELD_REQUIRED',
          `${field} es obligatorio`
        );
      }
    });
    if (this.type === BulkLoadConstants.TYPE_CATEGORY_CREATE) {
      ['IsDigital', 'IsCategoryDad'].forEach(field => {
        const value = this.text(payload[field]).toUpperCase();
        if (value !== '' && value !== 'S' && value !== 'N') {
          this.addLocalError(
            sheetName, rowNumber, '', field, value, 'FLAG_FORMAT',
            `${field} solo admite S o N`
          );
        }
      });
    }
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
          definition.valueField ?? 'Value', this.text(rawValue), 'NUMBER_FORMAT',
          'El valor debe ser numérico');
      } else if (this.type === BulkLoadConstants.TYPE_PRODUCT_PRICE) {
        const decimalPlaces = this.decimalPlaces(rawValue);
        if (numeric <= 0 || numeric > 99999999999999.99 || decimalPlaces > 2) {
          this.addLocalError(definition.productSheet, rowNumber, '',
            definition.valueField ?? 'Value', this.text(rawValue), 'PRICE_FORMAT',
            'El precio debe cumplir NUMERO(16,2) y ser mayor a cero');
        }
      } else if (!Number.isInteger(numeric) || numeric < 1 || numeric > 9999999) {
        this.addLocalError(definition.productSheet, rowNumber, '',
          definition.valueField ?? 'Value', this.text(rawValue), 'STOCK_FORMAT',
          'La cantidad debe cumplir NUMERO(7): entero entre 1 y 9999999');
      }
      result.push({
        RowNumber: rowNumber,
        ProductCod: productCod,
        Value: numeric === null ? this.text(rawValue) : String(numeric),
        BusinessKey: productCod,
        Payload: {
          ProductCod: productCod,
          [definition.valueField ?? 'Value']:
            numeric === null ? this.text(rawValue) : numeric
        }
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
      const productCod = row.ProductCod ?? '';
      if (products.has(productCod)) {
        this.addLocalError(this.definition().productSheet, row.RowNumber, '',
          'ProductCod', productCod, 'PRODUCT_DUPLICATED',
          'El producto está repetido');
      }
      products.add(productCod);
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
        valueField: 'NumPhysicalStock',
        templateFileName: 'FORMATO_CARGA_DE_STOCK.xlsx',
        requiresDestinations: true
      };
    }
    if (this.type === BulkLoadConstants.TYPE_PRODUCT_CREATE) {
      return {
        productSheet: 'PRODUCTOS',
        headers: [
          'ProductCod', 'ProductName', 'ProductDesc', 'BrandCod',
          'CategoryCod', 'BarCode', 'NumPrice', 'NumMaxStock', 'NumMinStock'
        ],
        types: [
          'TEXTO(20)', 'TEXTO(128)', 'TEXTO(256)', 'TEXTO(128)',
          'TEXTO(128)', 'TEXTO(20)', 'NUMERO(16,2)', 'NUMERO', 'NUMERO'
        ],
        labels: [
          'CÓDIGO', 'NOMBRE', 'DESCRIPCIÓN', 'MARCA',
          'CATEGORÍA', 'CÓDIGO DE BARRAS', 'PRECIO',
          'STOCK MÁXIMO', 'STOCK MÍNIMO'
        ],
        templateFileName: 'FORMATO_CARGA_PRODUCTOS.xlsx',
        requiresDestinations: false,
        useFirstSheet: true
      };
    }
    if (this.type === BulkLoadConstants.TYPE_BRAND_CREATE) {
      return {
        productSheet: 'MARCAS',
        headers: ['BrandCod', 'BrandName'],
        types: ['TEXTO(15)', 'TEXTO(100)'],
        labels: ['Código', 'Nombre'],
        templateFileName: 'FORMATO_CARGA_MARCAS.xlsx',
        requiresDestinations: false,
        useFirstSheet: true
      };
    }
    if (this.type === BulkLoadConstants.TYPE_CATEGORY_CREATE) {
      return {
        productSheet: 'CATEGORIAS',
        headers: [
          'CategoryCod', 'CategoryName', 'CategoryDadName',
          'IsDigital', 'IsCategoryDad'
        ],
        types: [
          'TEXTO(15)', 'TEXTO(150)', 'TEXTO(150)', 'TEXTO(1)', 'TEXTO(1)'
        ],
        labels: [
          'Código', 'Nombre', 'Categoría Padre',
          'Es Digital (S/N)', 'Es Categoría Padre (S/N)'
        ],
        templateFileName: 'FORMATO_CARGA_CATEGORIAS.xlsx',
        requiresDestinations: false,
        useFirstSheet: true
      };
    }
    return {
      productSheet: 'PRODUCTO_PRECIO',
      headers: ['ProductCod', 'NumPrice'],
      types: ['TEXTO(20)', 'NUMERO(16,2)'],
      labels: ['CODIGO DE PRODUCTO', 'PRECIO DE PRODUCTO'],
      valueField: 'NumPrice',
      templateFileName: 'FORMATO_CARGA_DE_PRECIOS.xlsx',
      requiresDestinations: true
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

  private resetResult(preserveCurrent: boolean = false): void {
    if (!preserveCurrent) {
      this.current = null;
      this.details = this.emptyPage();
      this.detailFilter = {
        Query: '',
        StoreCod: '',
        ProcessStatus: ''
      };
    }
    this.localErrors = [];
    this.backendErrors = [];
    this.sourceWorkbook = null;
  }

  private resetDetailFilterForCurrent(): void {
    this.detailFilter = {
      Query: '',
      StoreCod: '',
      ProcessStatus: this.current
        && BulkLoadConstants.isCorrectableError(this.current.Head)
        ? BulkLoadConstants.ERROR
        : ''
    };
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
