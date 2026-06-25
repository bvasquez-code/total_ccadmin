import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';

import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { ValidationHelper } from 'src/app/enterprise/shared/helper/ValidationHelper'; // ajusta tu ruta real

import { CounterfoilRegisterDto } from '../../model/dto/CounterfoilRegisterDto';
import { DocumentTypeDto } from '../../model/dto/DocumentTypeDto';
import { CounterfoilEntity } from '../../model/entity/CounterfoilEntity';
import { StoreEntity } from 'src/app/enterprise/shared/model/entity/StoreEntity';
import { CounterfoilService } from '../../service/CounterfoilService';


@Component({
  selector: 'app-createcounterfoil',
  templateUrl: './createcounterfoil.component.html'
})
export class CreatecounterfoilComponent implements OnInit {

  CounterfoilCod: string = "";

  register: CounterfoilRegisterDto = new CounterfoilRegisterDto();

  DocumentTypeList: DocumentTypeDto[] = [];
  Store: StoreEntity = new StoreEntity();

  // opciones fijas de grupo
  GroupDocumentOptions: { Code: string; Description: string }[] = [
    { Code: 'F', Description: 'Grupo facturas' },
    { Code: 'B', Description: 'Grupo boletas' },
    { Code: 'G', Description: 'Grupo guías' },
    { Code: 'O', Description: 'Sin grupos' }
  ];

  constructor(
    private counterfoilService: CounterfoilService,
    private router: Router,
    private toastrService: ToastrService
  ) {
    let urlTree: any = this.router.parseUrl(this.router.url);
    this.CounterfoilCod = urlTree.queryParams['CounterfoilCod'] ?? "";
  }

  ngOnInit(): void {
    this.findDataForm(this.CounterfoilCod);
  }

  async findDataForm(CounterfoilCod: string) {
    const rpt: ResponseWsDto = await this.counterfoilService.formData(CounterfoilCod);

    if (!rpt.ErrorStatus) {

      const counterfoil: CounterfoilEntity = rpt.DataAdditional?.find((e: any) => e.Name === "counterfoil")?.Data;
      const documentType: DocumentTypeDto[] = rpt.DataAdditional?.find((e: any) => e.Name === "documentType")?.Data;
      const store: StoreEntity = rpt.DataAdditional?.find((e: any) => e.Name === "store")?.Data;

      this.DocumentTypeList = documentType ?? [];
      this.Store = store ?? new StoreEntity();

      if (counterfoil) {
        this.register.counterfoil = counterfoil;
      } else {
        // defaults para nuevo
        this.register.counterfoil.IsAutomatic = 'S';
        this.register.counterfoil.GroupDocument = 'O';
      }

      // Relación 1 a 1 (talonario - tienda)
      this.register.counterfoilStore.CounterfoilCod = this.register.counterfoil.CounterfoilCod;
      this.register.counterfoilStore.StoreCod = this.Store.StoreCod;

      // si es nuevo, generar código al vuelo
      if (!this.CounterfoilCod || this.CounterfoilCod === '') {
        this.buildCounterfoilCod();
      }
    }
  }

  async buildCounterfoilCod() {
    const doc = (this.register.counterfoil.DocumentType ?? '').trim().toUpperCase();
    const serie = (this.register.counterfoil.Series ?? '').trim().toUpperCase();

    this.register.counterfoil.DocumentType = doc;
    this.register.counterfoil.Series = serie;

    if (doc.length === 2 && serie.length === 4) {
      const CounterfoilCod = `${doc}${serie}`;

      const result = await this.validateExistenceCounterfoil(serie);
      if (result) {
        this.register.counterfoil.CounterfoilCod = CounterfoilCod;
      } else {
        this.register.counterfoil.Series = '';
      }
    } else {
      this.register.counterfoil.CounterfoilCod = '';
    }
  }

  async validateExistenceCounterfoil(Series: string): Promise<boolean> {
    const rpt: ResponseWsDto = await this.counterfoilService.existsSeries(Series);
    if (!rpt.ErrorStatus && rpt.Data) {
      const result: boolean = rpt.Data;
      if (result) {
        this.toastrService.error("El talonario ya existe");
        return false;
      }
    }
    return true;
  }

  async save() {

    this.buildCounterfoilCod();

    // relación 1 a 1
    this.register.counterfoilStore.CounterfoilCod = this.register.counterfoil.CounterfoilCod;
    this.register.counterfoilStore.StoreCod = this.Store.StoreCod;

    if (!this.validate(this.register)) return;

    const rpt: ResponseWsDto = await this.counterfoilService.save(this.register);

    if (!rpt.ErrorStatus) {
      this.toastrService.success("Talonario guardado");
      this.router.navigate(["enterprise/cash/pages/listcounterfoil"]);
    }
  }

  validate(register: CounterfoilRegisterDto): boolean {
    try {
      // ===== counterfoil (DDL) =====
      ValidationHelper.validateIsNotEmpty(register.counterfoil.DocumentType, "Seleccione un tipo de documento");
      ValidationHelper.validLengthString(register.counterfoil.DocumentType, 2, "El tipo de documento debe tener 2 caracteres");
      ValidationHelper.isValidString(register.counterfoil.DocumentType, "Tipo de documento inválido (solo alfanumérico)");

      ValidationHelper.validateIsNotEmpty(register.counterfoil.Series, "Ingrese una serie");
      ValidationHelper.validLengthString(register.counterfoil.Series, 4, "La serie debe tener 4 caracteres");
      ValidationHelper.isValidString(register.counterfoil.Series, "Serie inválida (solo alfanumérico)");

      ValidationHelper.validateIsNotEmpty(register.counterfoil.CounterfoilCod, "No se pudo generar el código del talonario");
      ValidationHelper.validLengthString(register.counterfoil.CounterfoilCod, 6, "El código del talonario debe tener 6 caracteres");
      ValidationHelper.isValidString(register.counterfoil.CounterfoilCod, "Código de talonario inválido (solo alfanumérico)");

      // consistencia PK
      const expected = `${register.counterfoil.DocumentType}${register.counterfoil.Series}`;
      if (register.counterfoil.CounterfoilCod !== expected) {
        throw new Error("Inconsistencia: el código del talonario no coincide con TipoDoc + Serie");
      }

      ValidationHelper.validateIsNotEmpty(register.counterfoil.Correlative, "Ingrese correlativo");
      ValidationHelper.validNumber(register.counterfoil.Correlative, null, 0, "Correlativo no válido");

      ValidationHelper.validateIsNotEmpty(register.counterfoil.IsAutomatic, "Seleccione si es automático");
      ValidationHelper.validLengthString(register.counterfoil.IsAutomatic, 1, "IsAutomatic inválido");
      ValidationHelper.validateInList(register.counterfoil.IsAutomatic, ["S", "N"], "Automático debe ser S o N");

      // GroupDocument char(1) controlado por select
      ValidationHelper.validateIsNotEmpty(register.counterfoil.GroupDocument, "Seleccione un grupo de documento");
      ValidationHelper.validLengthString(register.counterfoil.GroupDocument, 1, "Grupo documento inválido");
      ValidationHelper.validateInList(register.counterfoil.GroupDocument, ["F", "B", "G", "O"], "Grupo documento inválido");

      // ===== counterfoil_store (1 tienda) =====
      ValidationHelper.validateIsNotEmpty(register.counterfoilStore.StoreCod, "No se encontró tienda default");
      ValidationHelper.validLengthString(register.counterfoilStore.StoreCod, 4, "StoreCod debe tener 4 caracteres");
      ValidationHelper.isValidString(register.counterfoilStore.StoreCod, "StoreCod inválido (solo alfanumérico)");

      return true;

    } catch (e: any) {
      this.toastrService.error(e.message);
      return false;
    }
  }
}
