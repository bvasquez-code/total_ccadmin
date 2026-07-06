import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { ValidationHelper } from 'src/app/enterprise/shared/helper/ValidationHelper';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { TaxAffectationEntity } from '../../model/entity/TaxAffectationEntity';
import { TaxEntity } from '../../model/entity/TaxEntity';
import { TaxAffectationService } from '../../service/TaxAffectationService';

@Component({
  selector: 'app-createtaxaffectation',
  templateUrl: './createtaxaffectation.component.html'
})
export class CreatetaxaffectationComponent implements OnInit {

  TaxAffectationCod: string = "";
  taxAffectation: TaxAffectationEntity = new TaxAffectationEntity();
  taxList: TaxEntity[] = [];
  txtTaxAffectationCodReadonly: boolean = false;

  constructor(
    private taxAffectationService: TaxAffectationService,
    private router: Router,
    private toastrService: ToastrService
  ) {
    const urlTree: any = this.router.parseUrl(this.router.url);
    this.TaxAffectationCod = urlTree.queryParams['TaxAffectationCod'] ?? "";
  }

  ngOnInit(): void {
    this.FindDataForm(this.TaxAffectationCod);
  }

  async FindDataForm(TaxAffectationCod: string): Promise<void> {
    const rpt: ResponseWsDto = await this.taxAffectationService.findDataForm(TaxAffectationCod);
    if (!rpt.ErrorStatus) {
      const item = rpt.DataAdditional?.find(e => e.Name === "taxAffectation")?.Data;
      this.taxList = rpt.DataAdditional?.find(e => e.Name === "taxList")?.Data ?? [];
      if (item) this.taxAffectation = item;
      if (TaxAffectationCod !== "") this.txtTaxAffectationCodReadonly = true;
      this.ensureDefaults();
    }
  }

  async Save(): Promise<void> {
    this.normalize();
    if (!this.validate()) return;

    const rpt: ResponseWsDto = await this.taxAffectationService.save(this.taxAffectation);
    if (!rpt.ErrorStatus) {
      this.toastrService.success("Operacion realizada con exito.");
      this.router.navigate(['/enterprise/system/pages/listtaxaffectation']);
    } else {
      this.toastrService.error(rpt.Message);
    }
  }

  validate(): boolean {
    try {
      ValidationHelper.validateIsNotEmpty(this.taxAffectation.TaxAffectationCod, "Debe ingresar codigo de afectacion");
      ValidationHelper.validLengthString(this.taxAffectation.TaxAffectationCod, 4, "El codigo solo puede tener 4 caracteres");
      ValidationHelper.validateIsNotEmpty(this.taxAffectation.Name, "Debe ingresar nombre");
      ValidationHelper.validateIsNotEmpty(this.taxAffectation.Description, "Debe ingresar descripcion");
      ValidationHelper.validateIsNotEmpty(this.taxAffectation.TaxCod, "Debe seleccionar tributo asociado");
      return true;
    } catch (e: any) {
      this.toastrService.error(e.message);
      return false;
    }
  }

  validateKeypress(event: KeyboardEvent): void {
    try {
      ValidationHelper.isValidString(event.key.toString(), "Error", /[a-zA-Z0-9]/);
    } catch (e: any) {
      event.preventDefault();
    }
  }

  private ensureDefaults(): void {
    if (!this.taxAffectation) this.taxAffectation = new TaxAffectationEntity();
    this.taxAffectation.IsTaxed = this.taxAffectation.IsTaxed || "N";
  }

  private normalize(): void {
    this.ensureDefaults();
    this.taxAffectation.TaxAffectationCod = (this.taxAffectation.TaxAffectationCod || "").toUpperCase();
    this.taxAffectation.TaxCod = (this.taxAffectation.TaxCod || "").toUpperCase();
  }
}
