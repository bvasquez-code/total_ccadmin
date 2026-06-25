import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { ValidationHelper } from 'src/app/enterprise/shared/helper/ValidationHelper';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { CurrencyEntity } from 'src/app/enterprise/shared/model/entity/CurrencyEntity';
import { CurrencyService } from '../../service/CurrencyService';

@Component({
  selector: 'app-createcurrency',
  templateUrl: './createcurrency.component.html'
})
export class CreatecurrencyComponent implements OnInit {

  CurrencyCod: string = "";
  currency: CurrencyEntity = new CurrencyEntity();
  txtCurrencyCodReadonly: boolean = false;

  constructor(
    private currencyService: CurrencyService,
    private router: Router,
    private toastrService: ToastrService
  ) {
    this.GetParamUrl(this.router);
  }

  ngOnInit(): void {
    this.FindDataForm(this.CurrencyCod);
  }

  GetParamUrl(router: Router): void {
    const urlTree: any = router.parseUrl(this.router.url);
    this.CurrencyCod = urlTree.queryParams['CurrencyCod'] ?? "";
  }

  async FindDataForm(CurrencyCod: string): Promise<void> {
    const rpt: ResponseWsDto = await this.currencyService.findDataForm(CurrencyCod);

    if (!rpt.ErrorStatus) {
      const item = rpt.DataAdditional?.find(e => e.Name === "currency")?.Data;

      if (item) this.currency = item;
      if (CurrencyCod !== "") this.txtCurrencyCodReadonly = true;
      this.ensureDefaults();
    }
  }

  async Save(): Promise<void> {
    if (!this.currency) this.currency = new CurrencyEntity();
    this.normalizeCurrency();

    if (!this.validate(this.currency)) return;

    const rpt: ResponseWsDto = await this.currencyService.save(this.currency);

    if (!rpt.ErrorStatus) {
      this.toastrService.success("Operacion realizada con exito.");
      this.router.navigate(['/enterprise/system/pages/listcurrency']);
    }
  }

  validate(currency: CurrencyEntity): boolean {
    try {
      ValidationHelper.validateIsNotEmpty(currency.CurrencyCod, "Debe ingresar un codigo de moneda");
      ValidationHelper.validLengthString(currency.CurrencyCod, 5, "El codigo de moneda solo puede tener 5 caracteres");
      ValidationHelper.validateIsNotEmpty(currency.CurrencyAbbr, "Debe ingresar una abreviatura");
      ValidationHelper.validLengthString(currency.CurrencyAbbr, 8, "La abreviatura solo puede tener 8 caracteres");
      ValidationHelper.validateIsNotEmpty(currency.CurrencySymbol, "Debe ingresar un simbolo");
      ValidationHelper.validLengthString(currency.CurrencySymbol, 5, "El simbolo solo puede tener 5 caracteres");
      ValidationHelper.validateIsNotEmpty(currency.CurrencyName, "Debe ingresar un nombre");
      ValidationHelper.validLengthString(currency.CurrencyName, 32, "El nombre solo puede tener 32 caracteres");
      ValidationHelper.validLengthString(currency.CurrencyDesc, 128, "La descripcion solo puede tener 128 caracteres");
      ValidationHelper.validateInList(currency.IsCurrencySystem, ["S", "N"], "Debe seleccionar si es moneda principal del sistema");
      ValidationHelper.validNumber(currency.NumExchangevalue, null, 0, "Debe ingresar un tipo de cambio valido");

      return true;
    } catch (e: any) {
      this.toastrService.error(e.message);
      return false;
    }
  }

  validateKeypress(event: KeyboardEvent, id: string): void {
    try {
      if (id === "txtCurrencyCod" || id === "txtCurrencyAbbr") {
        ValidationHelper.isValidString(event.key.toString(), "Error", /[a-zA-Z0-9]/);
      }
    } catch (e: any) {
      event.preventDefault();
    }
  }

  private ensureDefaults(): void {
    if (!this.currency) this.currency = new CurrencyEntity();
    this.currency.IsCurrencySystem = this.currency.IsCurrencySystem || "N";
    this.currency.NumExchangevalue = this.currency.NumExchangevalue ?? 0;
  }

  private normalizeCurrency(): void {
    this.ensureDefaults();
    this.currency.CurrencyCod = (this.currency.CurrencyCod || "").toUpperCase();
    this.currency.CurrencyAbbr = (this.currency.CurrencyAbbr || "").toUpperCase();
  }
}
