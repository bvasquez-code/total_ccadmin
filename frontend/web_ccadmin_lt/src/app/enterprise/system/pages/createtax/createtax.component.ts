import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { ValidationHelper } from 'src/app/enterprise/shared/helper/ValidationHelper';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { TaxEntity } from '../../model/entity/TaxEntity';
import { TaxService } from '../../service/TaxService';

@Component({
  selector: 'app-createtax',
  templateUrl: './createtax.component.html'
})
export class CreatetaxComponent implements OnInit {

  TaxCod: string = "";
  tax: TaxEntity = new TaxEntity();
  txtTaxCodReadonly: boolean = false;

  constructor(
    private taxService: TaxService,
    private router: Router,
    private toastrService: ToastrService
  ) {
    const urlTree: any = this.router.parseUrl(this.router.url);
    this.TaxCod = urlTree.queryParams['TaxCod'] ?? "";
  }

  ngOnInit(): void {
    this.FindDataForm(this.TaxCod);
  }

  async FindDataForm(TaxCod: string): Promise<void> {
    const rpt: ResponseWsDto = await this.taxService.findDataForm(TaxCod);
    if (!rpt.ErrorStatus) {
      const item = rpt.DataAdditional?.find(e => e.Name === "tax")?.Data;
      if (item) this.tax = item;
      if (TaxCod !== "") this.txtTaxCodReadonly = true;
      this.ensureDefaults();
    }
  }

  async Save(): Promise<void> {
    this.normalize();
    if (!this.validate()) return;

    const rpt: ResponseWsDto = await this.taxService.save(this.tax);
    if (!rpt.ErrorStatus) {
      this.toastrService.success("Operacion realizada con exito.");
      this.router.navigate(['/enterprise/system/pages/listtax']);
    } else {
      this.toastrService.error(rpt.Message);
    }
  }

  onCalculationTypeChange(): void {
    if (this.tax.TaxCalculationType === "F") {
      this.tax.TaxRateValue = 0;
    }
    if (this.tax.TaxCalculationType === "N") {
      this.tax.TaxRateValue = 0;
      this.tax.FixedUnitAmount = 0;
      this.tax.IsInformative = "S";
    }
  }

  validate(): boolean {
    try {
      ValidationHelper.validateIsNotEmpty(this.tax.TaxCod, "Debe ingresar codigo de tributo");
      ValidationHelper.validLengthString(this.tax.TaxCod, 8, "El codigo solo puede tener 8 caracteres");
      ValidationHelper.validateIsNotEmpty(this.tax.Name, "Debe ingresar nombre");
      ValidationHelper.validateIsNotEmpty(this.tax.Description, "Debe ingresar descripcion");
      ValidationHelper.validateIsNotEmpty(this.tax.TaxCalculationType, "Debe seleccionar tipo de calculo");
      ValidationHelper.validNumber(this.tax.TaxRateValue, null, 0, "Tasa no valida");
      ValidationHelper.validNumber(this.tax.FixedUnitAmount, null, 0, "Monto fijo no valido");
      ValidationHelper.validNumber(this.tax.CalculationOrder, null, 1, "Orden de calculo no valido");

      if (this.tax.TaxCalculationType === "F" && Number(this.tax.TaxRateValue || 0) > 0) {
        throw new Error("Un tributo fijo por unidad no debe tener tasa porcentual");
      }
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
    if (!this.tax) this.tax = new TaxEntity();
    this.tax.TaxCalculationType = this.tax.TaxCalculationType || "P";
    this.tax.IsInformative = this.tax.IsInformative || "N";
    this.tax.TaxRateValue = Number(this.tax.TaxRateValue || 0);
    this.tax.FixedUnitAmount = Number(this.tax.FixedUnitAmount || 0);
    this.tax.CalculationOrder = Number(this.tax.CalculationOrder || 100);
  }

  private normalize(): void {
    this.ensureDefaults();
    this.tax.TaxCod = (this.tax.TaxCod || "").toUpperCase();
    this.tax.SunatTaxCod = (this.tax.SunatTaxCod || "").toUpperCase();
  }
}
