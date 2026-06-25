import { Component, OnInit } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { ProductService } from '../../service/product.service';
import { ProductRegisterMassiveDto } from '../../model/dto/ProductRegisterMassiveDto';
import { ProductRegisterDto } from '../../model/dto/ProductRegisterDto';
import { ProductEntity } from '../../model/entity/ProductEntity';
import { ProductConfigEntity } from '../../model/entity/ProductConfigEntity';
import { ProductBarcodeEntity } from '../../model/entity/ProductBarcodeEntity';
import * as XLSX from 'xlsx';
import { BrandEntity } from '../../model/entity/BrandEntity';
import { CategoryEntity } from '../../model/entity/CategoryEntity';

@Component({
  selector: 'app-createproductmassive',
  templateUrl: './createproductmassive.component.html',
  styleUrls: ['./createproductmassive.component.css']
})
export class CreateproductmassiveComponent implements OnInit {

  public uploadFormat: any;
  public brandList: BrandEntity[] = [];
  public categoryList: CategoryEntity[] = [];
  public previewList: ProductRegisterDto[] = [];
  public excelDataPreview: any[] = []; // Para mostrar en el HTML tal como viene
  public failedProducts: { ProductCod: string, ErrorDesc: string }[] = [];

  constructor(
    private productService: ProductService,
    private toastr: ToastrService
  ) { }

  async ngOnInit() {
    await this.loadFormat();
  }

  async loadFormat() {
    try {
      const response = await this.productService.FindDataFormMassive();
      if (response && !response.ErrorStatus) {
        this.brandList = response.DataAdditional.find(x => x.Name === 'brandList')?.Data || [];
        this.categoryList = response.DataAdditional.find(x => x.Name === 'categoryList')?.Data || [];

        const docFormat = response.DataAdditional.find(x => x.Name === 'bulkUploadFormatProducts')?.Data;
        if (docFormat && docFormat.Content) {
          this.uploadFormat = JSON.parse(docFormat.Content);
        } else {
          this.toastr.error("No se encontró el formato de carga masiva");
        }
      } else {
        this.toastr.error(response?.Message || "Error al cargar configuración");
      }
    } catch (e) {
      this.toastr.error("Error al obtener datos iniciales");
    }
  }

  exportExcel() {
    if (!this.uploadFormat) {
      this.toastr.error("El formato de carga aún no está disponible.");
      return;
    }

    const exportData = [
      this.uploadFormat.headers,
      this.uploadFormat.types,
      this.uploadFormat.labels
    ];

    const ws: XLSX.WorkSheet = XLSX.utils.aoa_to_sheet(exportData);
    const wb: XLSX.WorkBook = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, this.uploadFormat.sheetName || "Sheet1");

    XLSX.writeFile(wb, "formato_carga_productos.xlsx");
  }

  uploadExcel(event: any) {
    const target: DataTransfer = <DataTransfer>(event.target);
    if (target.files.length !== 1) {
      this.toastr.error("No se puede usar múltiples archivos");
      return;
    }

    const file = target.files[0];
    const reader: FileReader = new FileReader();

    reader.onload = (e: any) => {
      const bstr: string = e.target.result;
      const wb: XLSX.WorkBook = XLSX.read(bstr, { type: 'binary' });

      // Cargar primera hoja
      const wsname: string = wb.SheetNames[0];
      const ws: XLSX.WorkSheet = wb.Sheets[wsname];

      // Convertir a JSON usando la primera fila (headers) como keys
      const data = XLSX.utils.sheet_to_json(ws) as any[];

      if (data.length < 2) {
        this.toastr.error("El archivo está vacío o no tiene suficientes filas.");
        return;
      }

      this.processExcelData(data);
    };

    reader.readAsBinaryString(file);

    // reset input
    event.target.value = '';
  }

  processExcelData(data: any[]) {
    // La fila 1 del excel son keys. En `data` index 0 son types (fila 2 del excel)
    // index 1 son labels (fila 3 del excel). A partir del index 2 son datos reales.
    const expectedHeaders = this.uploadFormat.headers;
    const types = this.uploadFormat.types;
    const expectedLabels = this.uploadFormat.labels;

    // Verificar si tiene la estructura de cabeceras correcta
    const jsonKeys = Object.keys(data[0] || {});
    for (let i = 0; i < expectedHeaders.length; i++) {
      if (!jsonKeys.includes(expectedHeaders[i])) {
        this.toastr.error(`Falta la columna "${expectedHeaders[i]}" en la primera fila.`);
        return;
      }
    }

    const rows = data.slice(2);
    const validPreview: ProductRegisterDto[] = [];
    const validExcelPreview: any[] = [];
    this.failedProducts = [];

    for (let rowIndex = 0; rowIndex < rows.length; rowIndex++) {
      const row = rows[rowIndex];
      // Skip empty rows completely
      if (!row || Object.keys(row).length === 0) {
        continue;
      }

      const dto = new ProductRegisterDto();
      // Instanciar objetos internos explícitamente para evitar nulls
      dto.product = new ProductEntity();
      dto.config = new ProductConfigEntity();
      dto.productBarcode = new ProductBarcodeEntity();

      const excelRow: any = { ...row };

      let rowErrors: string[] = [];

      for (let colIndex = 0; colIndex < expectedHeaders.length; colIndex++) {
        const propName = expectedHeaders[colIndex];
        let value = row[propName];
        const typeRule = types[colIndex];
        const label = expectedLabels[colIndex];

        if (value === undefined || value === null) {
          value = "";
        }

        // Validation
        if (typeRule.startsWith("TEXTO")) {
          const match = typeRule.match(/\((\d+)\)/);
          const maxLength = match ? parseInt(match[1], 10) : 0;

          value = value.toString();
          if (maxLength > 0 && value.length > maxLength) {
            rowErrors.push(`Columna "${label}": Excede ${maxLength} caracteres`);
          }
        } else if (typeRule === "NUMERO") {
          if (value !== "" && isNaN(Number(value))) {
            rowErrors.push(`Columna "${label}": Debe ser un número`);
          } else {
            value = value !== "" ? Number(value) : 0;
          }
        }

        // Additional Logic for BrandCod and CategoryCod matching
        if (propName === 'BrandCod') {
          if (this.getBrandCodLike(value.toString()) === "") {
            rowErrors.push(`Marca "${value}" no encontrada en el sistema`);
          }
        } else if (propName === 'CategoryCod') {
          if (this.getCategoryCodLike(value.toString()) === "") {
            rowErrors.push(`Categoría "${value}" no encontrada en el sistema`);
          }
        }

        // Mapping to DTO
        if (['ProductCod', 'ProductName', 'ProductDesc'].includes(propName)) {
          (dto.product as any)[propName] = value;
        } else if (propName === 'BarCode') {
          dto.productBarcode.BarCode = value.toString();
        } else if (['NumPrice', 'NumMaxStock', 'NumMinStock'].includes(propName)) {
          (dto.config as any)[propName] = value;
        } else if (propName === 'BrandCod') {
          dto.product.BrandCod = this.getBrandCodLike(value.toString());
        } else if (propName === 'CategoryCod') {
          dto.product.CategoryCod = this.getCategoryCodLike(value.toString());
        }
      }

      if (rowErrors.length > 0) {
        this.failedProducts.push({
          ProductCod: row['ProductCod'] || 'Desconocido',
          ErrorDesc: rowErrors.join(' | ')
        });
        continue;
      }

      validPreview.push(dto);
      validExcelPreview.push(excelRow);
    }

    this.previewList = validPreview;
    this.excelDataPreview = validExcelPreview;

    if (this.failedProducts.length > 0) {
      this.toastr.warning(`Se procesaron ${this.previewList.length} registros válidos. Hay ${this.failedProducts.length} registros con errores.`);
    } else {
      this.toastr.success(`Archivo procesado con éxito. ${this.previewList.length} registros listos.`);
    }
  }

  async saveMassive() {
    if (this.previewList.length === 0) {
      this.toastr.error("No hay registros válidos para cargar.");
      return;
    }

    // Set internal relationship fields like ProductCod in ProductBarcodeEntity 
    this.previewList.forEach(item => {
      item.productBarcode.ProductCod = item.product.ProductCod;
      item.config.ProductCod = item.product.ProductCod;
      item.product.BrandCod = this.getBrandCodLike(item.product.BrandCod);
      item.product.CategoryCod = this.getCategoryCodLike(item.product.CategoryCod);
    });

    const request = new ProductRegisterMassiveDto();
    request.productList = this.previewList;

    try {
      const response = await this.productService.SaveAll(request);
      if (response && !response.ErrorStatus) {
        this.toastr.success("Carga masiva realizada con éxito");
        this.previewList = [];
        this.excelDataPreview = [];
      } else {
        this.toastr.error(response?.Message || "Error al procesar la carga masiva");
      }
    } catch (e) {
      this.toastr.error("Ocurrió un error inesperado durante el guardado");
    }
  }

  getBrandCodLike(brandName: string): string {
    const brand = this.brandList.find(b => b.BrandName.toLowerCase().includes(brandName.toLowerCase()));
    return brand ? brand.BrandCod : "";
  }

  getCategoryCodLike(categoryName: string): string {
    const category = this.categoryList.find(c => c.CategoryName.toLowerCase().includes(categoryName.toLowerCase()));
    return category ? category.CategoryCod : "";
  }

  likeText(textContent: string, value: string): boolean {
    if (textContent === undefined || textContent === null || value === undefined || value === null) {
      return false;
    }
    return textContent.trim().toLocaleLowerCase().includes(value.trim().toLocaleLowerCase());
  }

  exportFailedProducts() {
    if (this.failedProducts.length === 0) {
      this.toastr.info("No hay productos con errores para exportar.");
      return;
    }

    const exportData = [
      ['Código Producto', 'Descripción del Error'] // Headers
    ];

    this.failedProducts.forEach(item => {
      exportData.push([item.ProductCod, item.ErrorDesc]);
    });

    const ws: XLSX.WorkSheet = XLSX.utils.aoa_to_sheet(exportData);
    const wb: XLSX.WorkBook = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, "Errores");

    XLSX.writeFile(wb, "errores_carga_productos.xlsx");
  }

}
