import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { BrandEntity } from 'src/app/enterprise/product/model/entity/BrandEntity';
import { CategoryEntity } from 'src/app/enterprise/product/model/entity/CategoryEntity';
import { ProductService } from 'src/app/enterprise/product/service/product.service';
import {
  BulkLoadError,
  BulkLoadParsedRequest,
  BulkLoadRegister,
  BulkLoadSourceRow
} from '../../model/BulkLoadModels';
import { BulkLoadConstants } from '../../model/BulkLoadConstants';
import { BulkLoadService } from '../../service/bulk-load.service';

interface ManualProductRow {
  BarCode: string;
  ProductCod: string;
  ProductName: string;
  ProductDesc: string;
  BrandCod: string;
  BrandSearch: string;
  CategoryCod: string;
  CategorySearch: string;
  NumPrice: string;
}

@Component({
  selector: 'app-createbulkloadproducts',
  templateUrl: './createbulkloadproducts.component.html',
  styleUrls: ['./createbulkloadproducts.component.css']
})
export class CreateBulkLoadProductsComponent implements OnInit {
  rows: ManualProductRow[] = Array.from(
    { length: 5 }, () => this.emptyRow()
  );
  brandList: BrandEntity[] = [];
  categoryList: CategoryEntity[] = [];
  errors: BulkLoadError[] = [];
  loadingLookups = false;
  saving = false;

  private correctionCode = '';
  private returnTo = '/enterprise/bulkload/pages/createbulkload';
  private readonly allowedReturnRoutes = [
    '/enterprise/bulkload/pages/createbulkload',
    '/enterprise/product/pages/createproductmassive'
  ];
  private readonly defaultMaxStock = 100;
  private readonly defaultMinStock = 50;

  constructor(
    private productService: ProductService,
    private bulkLoadService: BulkLoadService,
    private toastr: ToastrService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    const requestedReturnRoute = this.route.snapshot.queryParamMap.get('returnTo');
    if (requestedReturnRoute
      && this.allowedReturnRoutes.includes(requestedReturnRoute)) {
      this.returnTo = requestedReturnRoute;
    }
    void this.loadLookups();
  }

  addRow(): void {
    this.rows.push(this.emptyRow());
  }

  trackByIndex(index: number): number {
    return index;
  }

  brandOptionValue(brand: BrandEntity): string {
    return `${brand.BrandName} (${brand.BrandCod})`;
  }

  categoryOptionValue(category: CategoryEntity): string {
    return `${category.CategoryName} (${category.CategoryCod})`;
  }

  onBrandInput(row: ManualProductRow): void {
    const input = this.clean(row.BrandSearch).toLowerCase();
    const selected = this.brandList.find(brand =>
      this.brandOptionValue(brand).toLowerCase() === input
      || this.clean(brand.BrandCod).toLowerCase() === input
      || this.clean(brand.BrandName).toLowerCase() === input
    );
    row.BrandCod = selected?.BrandCod ?? '';
    if (selected) row.BrandSearch = this.brandOptionValue(selected);
  }

  onCategoryInput(row: ManualProductRow): void {
    const input = this.clean(row.CategorySearch).toLowerCase();
    const selected = this.categoryList.find(category =>
      this.categoryOptionValue(category).toLowerCase() === input
      || this.clean(category.CategoryCod).toLowerCase() === input
      || this.clean(category.CategoryName).toLowerCase() === input
    );
    row.CategoryCod = selected?.CategoryCod ?? '';
    if (selected) row.CategorySearch = this.categoryOptionValue(selected);
  }

  rowErrors(rowNumber: number): string[] {
    return this.errors
      .filter(error => error.RowNumber === rowNumber)
      .map(error => error.ErrorDetail);
  }

  async submit(): Promise<void> {
    if (this.saving) return;

    const populatedRows = this.rows.filter(row => !this.isEmpty(row));
    if (populatedRows.length === 0) {
      this.toastr.warning('Debe ingresar al menos un producto');
      return;
    }

    this.saving = true;
    this.errors = [];
    try {
      const codesGenerated = await this.generateMissingProductCodes(populatedRows);
      if (!codesGenerated) return;

      const request = this.buildRequest();
      const response = this.correctionCode
        ? await this.bulkLoadService.correctParsed(this.correctionCode, request)
        : await this.bulkLoadService.saveParsed(request);
      if (response.ErrorStatus) {
        this.toastr.error(response.Message);
        return;
      }

      const register = response.Data as BulkLoadRegister;
      this.correctionCode = register.Head.BulkLoadCod;
      this.errors = register.ErrorList ?? [];
      if (this.errors.length > 0) {
        this.toastr.warning(
          `Se encontraron ${this.errors.length} error(es). Corrija las filas indicadas.`
        );
        return;
      }

      this.toastr.success('Productos validados. Revise el preview antes de confirmar');
      await this.router.navigate(
        [this.returnTo],
        { queryParams: { BulkLoadCod: register.Head.BulkLoadCod } }
      );
    } catch (error) {
      this.toastr.error(this.errorMessage(error));
    } finally {
      this.saving = false;
    }
  }

  async cancel(): Promise<void> {
    await this.router.navigate([this.returnTo]);
  }

  private async loadLookups(): Promise<void> {
    this.loadingLookups = true;
    try {
      const response = await this.productService.FindDataFormMassive();
      if (response.ErrorStatus) {
        this.toastr.error(response.Message);
        return;
      }
      this.brandList = response.DataAdditional
        .find(item => item.Name === 'brandList')?.Data ?? [];
      this.categoryList = response.DataAdditional
        .find(item => item.Name === 'categoryList')?.Data ?? [];
    } catch (error) {
      this.toastr.error(this.errorMessage(error));
    } finally {
      this.loadingLookups = false;
    }
  }

  private async generateMissingProductCodes(
    rows: ManualProductRow[]
  ): Promise<boolean> {
    for (const row of rows) {
      if (this.clean(row.ProductCod) !== '') continue;

      const response = await this.productService.GenerateProductCode();
      if (response.ErrorStatus) {
        this.toastr.error(response.Message);
        return false;
      }
      const generatedCode = this.clean(response.Data);
      if (generatedCode === '') {
        this.toastr.error('No se pudo generar el código de uno de los productos');
        return false;
      }
      row.ProductCod = generatedCode;
    }
    return true;
  }

  private buildRequest(): BulkLoadParsedRequest {
    const rowList: BulkLoadSourceRow[] = [];
    this.rows.forEach((row, index) => {
      if (this.isEmpty(row)) return;

      const productCod = this.clean(row.ProductCod);
      rowList.push({
        RowNumber: index + 4,
        BusinessKey: productCod,
        ProductCod: productCod,
        Payload: {
          ProductCod: productCod,
          ProductName: this.clean(row.ProductName),
          ProductDesc: this.clean(row.ProductDesc),
          BrandCod: row.BrandCod || this.clean(row.BrandSearch),
          CategoryCod: row.CategoryCod || this.clean(row.CategorySearch),
          BarCode: this.clean(row.BarCode),
          NumPrice: this.numberValue(row.NumPrice),
          NumMaxStock: this.defaultMaxStock,
          NumMinStock: this.defaultMinStock
        }
      });
    });

    return {
      BulkLoadType: BulkLoadConstants.TYPE_PRODUCT_CREATE,
      SchemaVersion: 1,
      OriginalFileName: 'REGISTRO_MANUAL_PRODUCTOS.json',
      RowList: rowList,
      StoreList: []
    };
  }

  private isEmpty(row: ManualProductRow): boolean {
    return [
      row.BarCode,
      row.ProductCod,
      row.ProductName,
      row.ProductDesc,
      row.BrandCod,
      row.BrandSearch,
      row.CategoryCod,
      row.CategorySearch,
      row.NumPrice
    ].every(value => this.clean(value) === '');
  }

  private numberValue(value: string): number | string {
    const text = this.clean(value).replace(',', '.');
    if (text === '') return 0;
    if (!/^[+-]?\d+(?:\.\d+)?$/.test(text)) return this.clean(value);
    return Number(text);
  }

  private emptyRow(): ManualProductRow {
    return {
      BarCode: '',
      ProductCod: '',
      ProductName: '',
      ProductDesc: '',
      BrandCod: '',
      BrandSearch: '',
      CategoryCod: '',
      CategorySearch: '',
      NumPrice: ''
    };
  }

  private clean(value: unknown): string {
    return value === null || value === undefined ? '' : String(value).trim();
  }

  private errorMessage(error: unknown): string {
    if (error instanceof Error) return error.message;
    return 'No fue posible procesar los productos';
  }
}
