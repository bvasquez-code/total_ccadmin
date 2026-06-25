import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { ValidationHelper } from 'src/app/enterprise/shared/helper/ValidationHelper';
import { GenericCatalogDto } from 'src/app/enterprise/shared/model/dto/GenericCatalogDto';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { PaymentMethodEntity } from 'src/app/enterprise/shared/model/entity/PaymentMethodEntity';
import { AppFileEntity } from '../../model/entity/AppFileEntity';
import { PaymentMethodService } from '../../service/PaymentMethodService';
import { AppfileComponentRequestDto } from '../../model/dto/AppfileComponentRequestDto';

@Component({
  selector: 'app-createpaymentmethod',
  templateUrl: './createpaymentmethod.component.html'
})
export class CreatepaymentmethodComponent implements OnInit {

  PaymentMethodCod: string = "";
  paymentMethod: PaymentMethodEntity = new PaymentMethodEntity();
  paymentMethodTypeList: GenericCatalogDto[] = [];
  txtPaymentMethodCodReadonly: boolean = false;
  appfileComponentRequest: AppfileComponentRequestDto = new AppfileComponentRequestDto();

  constructor(
    private paymentMethodService: PaymentMethodService,
    private router: Router,
    private toastrService: ToastrService
  ) {
    this.appfileComponentRequest.groupTypeFile = 3;
    this.GetParamUrl(this.router);
  }

  ngOnInit(): void {
    this.FindDataForm(this.PaymentMethodCod);
  }

  GetParamUrl(router: Router): void {
    const urlTree: any = router.parseUrl(this.router.url);
    this.PaymentMethodCod = urlTree.queryParams['PaymentMethodCod'] ?? "";
  }

  async FindDataForm(PaymentMethodCod: string): Promise<void> {
    const rpt: ResponseWsDto = await this.paymentMethodService.findDataForm(PaymentMethodCod);

    if (!rpt.ErrorStatus) {
      const item = rpt.DataAdditional?.find(e => e.Name === "paymentMethod")?.Data;
      this.paymentMethodTypeList = rpt.DataAdditional?.find(e => e.Name === "paymentMethodType")?.Data ?? [];

      if (item) this.paymentMethod = item;
      if (PaymentMethodCod !== "") this.txtPaymentMethodCodReadonly = true;
      this.ensureDefaults();
    }
  }

  async Save(): Promise<void> {
    if (!this.paymentMethod) this.paymentMethod = new PaymentMethodEntity();
    this.normalizePaymentMethod();

    if (!this.validate(this.paymentMethod)) return;

    const rpt: ResponseWsDto = await this.paymentMethodService.save(this.paymentMethod);

    if (!rpt.ErrorStatus) {
      this.toastrService.success("Operacion realizada con exito.");
      this.router.navigate(['/enterprise/system/pages/listpaymentmethod']);
    }
  }

  validate(paymentMethod: PaymentMethodEntity): boolean {
    try {
      ValidationHelper.validateIsNotEmpty(paymentMethod.PaymentMethodCod, "Debe ingresar un codigo de metodo de pago");
      ValidationHelper.validLengthString(paymentMethod.PaymentMethodCod, 8, "El codigo solo puede tener 8 caracteres");
      ValidationHelper.validateIsNotEmpty(paymentMethod.Name, "Debe ingresar un nombre");
      ValidationHelper.validLengthString(paymentMethod.Name, 32, "El nombre solo puede tener 32 caracteres");
      ValidationHelper.validateIsNotEmpty(paymentMethod.Description, "Debe ingresar una descripcion");
      ValidationHelper.validLengthString(paymentMethod.Description, 64, "La descripcion solo puede tener 64 caracteres");
      ValidationHelper.validateIsNotEmpty(paymentMethod.PaymentMethodType, "Debe seleccionar un tipo de metodo de pago");

      return true;
    } catch (e: any) {
      this.toastrService.error(e.message);
      return false;
    }
  }

  validateKeypress(event: KeyboardEvent, id: string): void {
    try {
      if (id === "txtPaymentMethodCod") {
        ValidationHelper.isValidString(event.key.toString(), "Error", /[a-zA-Z0-9]/);
      }
    } catch (e: any) {
      event.preventDefault();
    }
  }

  ResponseResultFormAppFile(event: any): void {
    const appFile: AppFileEntity = event;

    if (!appFile) return;

    this.ensureDefaults();
    this.paymentMethod.FileCod = appFile.FileCod;
    this.paymentMethod.Route = appFile.Route;
  }

  clearImage(): void {
    this.ensureDefaults();
    this.paymentMethod.FileCod = "";
    this.paymentMethod.Route = "";
  }

  private ensureDefaults(): void {
    if (!this.paymentMethod) this.paymentMethod = new PaymentMethodEntity();
    this.paymentMethod.PaymentMethodType = this.paymentMethod.PaymentMethodType || "";
    this.paymentMethod.FileCod = this.paymentMethod.FileCod || "";
    this.paymentMethod.Route = this.paymentMethod.Route || "";
  }

  private normalizePaymentMethod(): void {
    this.ensureDefaults();
    this.paymentMethod.PaymentMethodCod = (this.paymentMethod.PaymentMethodCod || "").toUpperCase();
  }
}
