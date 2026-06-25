import { Component, OnInit } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { CategoryService } from '../../service/category.service';
import { CategoryRegisterMassiveDto } from '../../model/dto/CategoryRegisterMassiveDto';
import { CategoryEntity } from '../../model/entity/CategoryEntity';
import * as XLSX from 'xlsx';

@Component({
  selector: 'app-createcategorymassive',
  templateUrl: './createcategorymassive.component.html',
  styleUrls: ['./createcategorymassive.component.css']
})
export class CreatecategorymassiveComponent implements OnInit {

  public uploadFormat: any;
  public categoryDadList: CategoryEntity[] = [];
  public previewList: CategoryEntity[] = [];
  public excelDataPreview: any[] = [];
  public failedProducts: { Codigo: string, ErrorDesc: string }[] = [];

  constructor(
    private categoryService: CategoryService,
    private toastr: ToastrService
  ) { }

  async ngOnInit() {
    this.initFormat();
    await this.loadFormat();
  }

  initFormat() {
    this.uploadFormat = {
      headers: ["CategoryCod", "CategoryName", "CategoryDadName", "IsDigital", "IsCategoryDad"],
      types: ["TEXTO(15)", "TEXTO(150)", "TEXTO(150)", "TEXTO(1)", "TEXTO(1)"],
      labels: ["Código", "Nombre", "Categoría Padre", "Es Digital (S/N)", "Es Categoría Padre (S/N)"]
    };
  }

  async loadFormat() {
    try {
      const response = await this.categoryService.FindDataFormMassive();
      if (response && !response.ErrorStatus) {
        this.categoryDadList = response.DataAdditional.find((x: any) => x.Name === 'categoryDadList')?.Data || [];
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
    XLSX.utils.book_append_sheet(wb, ws, "Formato Categoria");

    XLSX.writeFile(wb, "formato_carga_categorias.xlsx");
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

      const wsname: string = wb.SheetNames[0];
      const ws: XLSX.WorkSheet = wb.Sheets[wsname];

      const data = XLSX.utils.sheet_to_json(ws) as any[];

      if (data.length < 2) {
        this.toastr.error("El archivo está vacío o no tiene suficientes filas.");
        return;
      }

      this.processExcelData(data);
    };

    reader.readAsBinaryString(file);
    event.target.value = '';
  }

  processExcelData(data: any[]) {
    const expectedHeaders = this.uploadFormat.headers;
    const types = this.uploadFormat.types;
    const expectedLabels = this.uploadFormat.labels;

    const jsonKeys = Object.keys(data[0] || {});
    for (let i = 0; i < expectedHeaders.length; i++) {
      if (!jsonKeys.includes(expectedHeaders[i])) {
        this.toastr.error(`Falta la columna "${expectedHeaders[i]}" en la primera fila.`);
        return;
      }
    }

    const rows = data.slice(2);
    const validPreview: CategoryEntity[] = [];
    const validExcelPreview: any[] = [];
    this.failedProducts = [];

    for (let rowIndex = 0; rowIndex < rows.length; rowIndex++) {
      const row = rows[rowIndex];
      if (!row || Object.keys(row).length === 0) {
        continue;
      }

      const entity = new CategoryEntity();
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
        }

        // Logic for CategoryDadCod
        if (propName === 'CategoryDadName') {
          if (value !== "" && this.getCategoryDadCodLike(value.toString()) === "") {
            rowErrors.push(`Categoría Padre "${value}" no encontrada en el sistema`);
          }
        }

        (entity as any)[propName] = value;
        if (propName === 'CategoryDadName') {
          entity.CategoryDadCod = this.getCategoryDadCodLike(value.toString());
        }
      }

      if (rowErrors.length > 0) {
        this.failedProducts.push({
          Codigo: row['CategoryDadName'] || 'Desconocido',
          ErrorDesc: rowErrors.join(' | ')
        });
        continue;
      }

      validPreview.push(entity);
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

    const request = new CategoryRegisterMassiveDto();
    request.categoryList = this.previewList;

    try {
      const response = await this.categoryService.SaveAll(request);
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

  getCategoryDadCodLike(categoryName: string): string {
    if (!categoryName) return "";
    const category = this.categoryDadList.find(c => c.CategoryName?.toLowerCase().includes(categoryName.toLowerCase()));
    return category ? category.CategoryCod : "";
  }

  exportFailedProducts() {
    if (this.failedProducts.length === 0) {
      this.toastr.info("No hay categorías con errores para exportar.");
      return;
    }

    const exportData = [
      ['Código Categoría', 'Descripción del Error']
    ];

    this.failedProducts.forEach(item => {
      exportData.push([item.Codigo, item.ErrorDesc]);
    });

    const ws: XLSX.WorkSheet = XLSX.utils.aoa_to_sheet(exportData);
    const wb: XLSX.WorkBook = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, "Errores");

    XLSX.writeFile(wb, "errores_carga_categorias.xlsx");
  }

}
