import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { ValidationHelper } from 'src/app/enterprise/shared/helper/ValidationHelper';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { TableSequenceEntity } from 'src/app/enterprise/shared/model/entity/TableSequenceEntity';
import { TableSequenceService } from '../../service/TableSequenceService';

@Component({
  selector: 'app-createtablesequence',
  templateUrl: './createtablesequence.component.html'
})
export class CreatetablesequenceComponent implements OnInit {

  SequenceTableType: string = "";
  tableSequence: TableSequenceEntity = new TableSequenceEntity();
  sequenceTableTypeList: string[] = [];
  isEdit: boolean = false;

  constructor(
    private tableSequenceService: TableSequenceService,
    private router: Router,
    private toastrService: ToastrService
  ) {
    this.GetParamUrl(this.router);
  }

  ngOnInit(): void {
    this.FindDataForm(this.SequenceTableType);
  }

  GetParamUrl(router: Router): void {
    const urlTree: any = router.parseUrl(this.router.url);
    this.SequenceTableType = (urlTree.queryParams['SequenceTableType'] ?? "").toString().trim();
    this.isEdit = this.SequenceTableType !== "";
  }

  async FindDataForm(SequenceTableType: string): Promise<void> {
    const rpt: ResponseWsDto = await this.tableSequenceService.findDataForm(SequenceTableType);

    if (!rpt.ErrorStatus) {
      const item = rpt.DataAdditional?.find(e => e.Name === "tableSequence")?.Data;
      this.sequenceTableTypeList = rpt.DataAdditional?.find(e => e.Name === "sequenceTableTypeList")?.Data ?? [];

      if (item) {
        this.tableSequence = item;
      }
      this.ensureDefaults();
    }
  }

  async Save(): Promise<void> {
    this.normalizeTableSequence();

    if (!this.validate(this.tableSequence)) return;

    const rpt: ResponseWsDto = await this.tableSequenceService.save(this.tableSequence);

    if (!rpt.ErrorStatus) {
      this.toastrService.success("Operacion realizada con exito.");
      this.router.navigate(['/enterprise/system/pages/listtablesequence']);
    }
  }

  validate(tableSequence: TableSequenceEntity): boolean {
    try {
      ValidationHelper.validateIsNotEmpty(tableSequence.SequenceTrx, "Debe ingresar la secuencia");
      ValidationHelper.validNumber(tableSequence.SequenceTrx, null, 0, "Secuencia invalida");
      ValidationHelper.validateIsNotEmpty(tableSequence.SequenceTableType, "Debe ingresar el tipo de tabla");
      ValidationHelper.validLengthString(tableSequence.SequenceTableType, 32, "El tipo de tabla solo puede tener 32 caracteres");
      ValidationHelper.validateIsNotEmpty(tableSequence.Prefix, "Debe ingresar un prefijo");
      ValidationHelper.validLengthString(tableSequence.Prefix, 2, "El prefijo solo puede tener 2 caracteres");
      ValidationHelper.validateIsNotEmpty(tableSequence.length, "Debe ingresar la longitud");
      ValidationHelper.validNumber(tableSequence.length, 32, 1, "Longitud invalida");
      ValidationHelper.validateIsNotEmpty(tableSequence.UsePrefix, "Debe seleccionar si usa prefijo");
      ValidationHelper.validateInList(tableSequence.UsePrefix, ["S", "N"], "Uso de prefijo invalido");

      if (tableSequence.UsePrefix === "S" && tableSequence.Prefix.length >= tableSequence.length) {
        throw new Error("La longitud debe ser mayor que la longitud del prefijo");
      }

      return true;
    } catch (e: any) {
      this.toastrService.error(e.message);
      return false;
    }
  }

  validateKeypress(event: KeyboardEvent, id: string): void {
    try {
      if (id === "txtSequenceTableType") {
        ValidationHelper.isValidString(event.key.toString(), "Error", /[a-zA-Z0-9_]/);
      }
      if (id === "txtPrefix") {
        ValidationHelper.isValidString(event.key.toString(), "Error", /[a-zA-Z]/);
      }
      if (id === "txtNumber") {
        ValidationHelper.isValidString(event.key.toString(), "Error", /[0-9]/);
      }
    } catch (e: any) {
      event.preventDefault();
    }
  }

  private ensureDefaults(): void {
    if (!this.tableSequence) this.tableSequence = new TableSequenceEntity();
    this.tableSequence.SequenceTrx = Number(this.tableSequence.SequenceTrx || 0);
    this.tableSequence.length = Number(this.tableSequence.length || 8);
    this.tableSequence.Prefix = this.tableSequence.Prefix || "";
    this.tableSequence.SequenceTableType = this.tableSequence.SequenceTableType || "";
    this.tableSequence.UsePrefix = this.tableSequence.UsePrefix || "S";
  }

  private normalizeTableSequence(): void {
    this.ensureDefaults();
    this.tableSequence.Prefix = (this.tableSequence.Prefix || "").toUpperCase();
    this.tableSequence.SequenceTableType = (this.tableSequence.SequenceTableType || "").trim();
    this.tableSequence.UsePrefix = (this.tableSequence.UsePrefix || "").toUpperCase();
    this.tableSequence.SequenceTrx = Number(this.tableSequence.SequenceTrx || 0);
    this.tableSequence.length = Number(this.tableSequence.length || 0);
  }
}
