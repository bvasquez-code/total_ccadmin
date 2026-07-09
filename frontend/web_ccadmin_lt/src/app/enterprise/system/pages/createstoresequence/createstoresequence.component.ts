import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { ValidationHelper } from 'src/app/enterprise/shared/helper/ValidationHelper';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { StoreSequenceEntity } from 'src/app/enterprise/shared/model/entity/StoreSequenceEntity';
import { StoreSequenceService } from '../../service/StoreSequenceService';

@Component({
  selector: 'app-createstoresequence',
  templateUrl: './createstoresequence.component.html'
})
export class CreatestoresequenceComponent implements OnInit {

  StoreCod: string = "";
  PeriodId: number = 0;
  SequenceTableType: string = "";
  storeSequence: StoreSequenceEntity = new StoreSequenceEntity();
  sequenceTableTypeList: string[] = [];
  isEdit: boolean = false;

  constructor(
    private storeSequenceService: StoreSequenceService,
    private router: Router,
    private toastrService: ToastrService
  ) {
    this.GetParamUrl(this.router);
  }

  ngOnInit(): void {
    this.FindDataForm(this.StoreCod, this.PeriodId, this.SequenceTableType);
  }

  GetParamUrl(router: Router): void {
    const urlTree: any = router.parseUrl(this.router.url);
    this.StoreCod = urlTree.queryParams['StoreCod'] ?? "";
    this.PeriodId = Number(urlTree.queryParams['PeriodId'] ?? 0);
    this.SequenceTableType = urlTree.queryParams['SequenceTableType'] ?? "";
    this.isEdit = this.StoreCod !== "" && this.PeriodId > 0 && this.SequenceTableType !== "";
  }

  async FindDataForm(StoreCod: string, PeriodId: number, SequenceTableType: string): Promise<void> {
    const rpt: ResponseWsDto = await this.storeSequenceService.findDataForm(
      StoreCod,
      PeriodId > 0 ? PeriodId : null,
      SequenceTableType
    );

    if (!rpt.ErrorStatus) {
      const item = rpt.DataAdditional?.find(e => e.Name === "storeSequence")?.Data;
      const activePeriod = rpt.DataAdditional?.find(e => e.Name === "activePeriod")?.Data;
      this.sequenceTableTypeList = rpt.DataAdditional?.find(e => e.Name === "sequenceTableTypeList")?.Data ?? [];

      if (item) {
        this.storeSequence = item;
      } else if (activePeriod) {
        this.storeSequence.PeriodId = activePeriod.PeriodId;
      }
      this.ensureDefaults();
    }
  }

  async Save(): Promise<void> {
    this.normalizeStoreSequence();

    if (!this.validate(this.storeSequence)) return;

    const rpt: ResponseWsDto = await this.storeSequenceService.save(this.storeSequence);

    if (!rpt.ErrorStatus) {
      this.toastrService.success("Operacion realizada con exito.");
      this.router.navigate(['/enterprise/system/pages/liststoresequence']);
    }
  }

  validate(storeSequence: StoreSequenceEntity): boolean {
    try {
      if (this.isEdit) {
        ValidationHelper.validateIsNotEmpty(storeSequence.StoreCod, "Debe ingresar una tienda");
        ValidationHelper.validLengthString(storeSequence.StoreCod, 4, "La tienda solo puede tener 4 caracteres");
      }
      ValidationHelper.validateIsNotEmpty(storeSequence.PeriodId, "Debe ingresar un periodo");
      ValidationHelper.validNumber(storeSequence.PeriodId, null, 1, "Periodo invalido");
      ValidationHelper.validateIsNotEmpty(storeSequence.SequenceTableType, "Debe ingresar el tipo de tabla");
      ValidationHelper.validLengthString(storeSequence.SequenceTableType, 32, "El tipo de tabla solo puede tener 32 caracteres");
      ValidationHelper.validateIsNotEmpty(storeSequence.Prefix, "Debe ingresar un prefijo");
      ValidationHelper.validLengthString(storeSequence.Prefix, 2, "El prefijo solo puede tener 2 caracteres");
      ValidationHelper.validateIsNotEmpty(storeSequence.SequenceTrx, "Debe ingresar la secuencia");
      ValidationHelper.validNumber(storeSequence.SequenceTrx, null, 0, "Secuencia invalida");
      ValidationHelper.validateIsNotEmpty(storeSequence.SequenceLength, "Debe ingresar la longitud");
      ValidationHelper.validNumber(storeSequence.SequenceLength, 32, 1, "Longitud invalida");

      return true;
    } catch (e: any) {
      this.toastrService.error(e.message);
      return false;
    }
  }

  validateKeypress(event: KeyboardEvent, id: string): void {
    try {
      if (id === "txtStoreCod") {
        ValidationHelper.isValidString(event.key.toString(), "Error", /[a-zA-Z0-9]/);
      }
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
    if (!this.storeSequence) this.storeSequence = new StoreSequenceEntity();
    this.storeSequence.StoreCod = this.storeSequence.StoreCod || "";
    this.storeSequence.SequenceTrx = Number(this.storeSequence.SequenceTrx || 0);
    this.storeSequence.SequenceLength = Number(this.storeSequence.SequenceLength || 7);
    this.storeSequence.Prefix = this.storeSequence.Prefix || "";
    this.storeSequence.SequenceTableType = this.storeSequence.SequenceTableType || "";
  }

  private normalizeStoreSequence(): void {
    this.ensureDefaults();
    this.storeSequence.StoreCod = (this.storeSequence.StoreCod || "").toUpperCase();
    this.storeSequence.Prefix = (this.storeSequence.Prefix || "").toUpperCase();
    this.storeSequence.SequenceTableType = (this.storeSequence.SequenceTableType || "").trim();
    this.storeSequence.PeriodId = Number(this.storeSequence.PeriodId || 0);
    this.storeSequence.SequenceTrx = Number(this.storeSequence.SequenceTrx || 0);
    this.storeSequence.SequenceLength = Number(this.storeSequence.SequenceLength || 0);
  }
}
