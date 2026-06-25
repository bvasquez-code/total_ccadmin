import { Component, OnInit } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { BrandService } from '../../service/brand.service';
import { BrandRegisterMassiveDto } from '../../model/dto/BrandRegisterMassiveDto';
import { BrandEntity } from '../../model/entity/BrandEntity';
import * as XLSX from 'xlsx';

@Component({
  selector: 'app-createbrandmassive',
  templateUrl: './createbrandmassive.component.html',
  styleUrls: ['./createbrandmassive.component.css']
})
export class CreatebrandmassiveComponent implements OnInit {

  public uploadFormat: any;
  public previewList: BrandEntity[] = [];
  public excelDataPreview: any[] = []; 
  public failedProducts: { Codigo: string, ErrorDesc: string }[] = [];

  constructor(
    private brandService: BrandService,
    private toastr: ToastrService
  ) { }

  ngOnInit() {
    this.initFormat();
  }

  initFormat() {
    this.uploadFormat = {
      headers: ["BrandCod", "BrandName"],
      types: ["TEXTO(15)", "TEXTO(100)"],
      labels: ["Código", "Nombre"]
    };
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
    XLSX.utils.book_append_sheet(wb, ws, "Formato Marca");

    XLSX.writeFile(wb, "formato_carga_marcas.xlsx");
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
    const validPreview: BrandEntity[] = [];
    const validExcelPreview: any[] = [];
    this.failedProducts = [];

    for (let rowIndex = 0; rowIndex < rows.length; rowIndex++) {
      const row = rows[rowIndex];
      if (!row || Object.keys(row).length === 0) {
        continue;
      }

      const entity = new BrandEntity();
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

        (entity as any)[propName] = value;
      }

      if (rowErrors.length > 0) {
        this.failedProducts.push({
          Codigo: row['BrandCod'] || 'Desconocido',
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

    const request = new BrandRegisterMassiveDto();
    request.brandList = this.previewList;

    try {
      const response = await this.brandService.SaveAll(request);
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

  exportFailedProducts() {
    if (this.failedProducts.length === 0) {
      this.toastr.info("No hay marcas con errores para exportar.");
      return;
    }

    const exportData = [
      ['Código Marca', 'Descripción del Error'] 
    ];

    this.failedProducts.forEach(item => {
      exportData.push([item.Codigo, item.ErrorDesc]);
    });

    const ws: XLSX.WorkSheet = XLSX.utils.aoa_to_sheet(exportData);
    const wb: XLSX.WorkBook = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, "Errores");

    XLSX.writeFile(wb, "errores_carga_marcas.xlsx");
  }

}
