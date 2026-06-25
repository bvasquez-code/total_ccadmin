import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { BrandService } from '../../service/brand.service';
import { IRegisterForm } from 'src/app/enterprise/shared/interface/IRegisterForm';
import { BrandEntity } from '../../model/entity/BrandEntity';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { ValidationHelper } from 'src/app/enterprise/shared/helper/ValidationHelper';

@Component({
  selector: 'app-createbrand',
  templateUrl: './createbrand.component.html'
})
export class CreatebrandComponent implements OnInit, IRegisterForm<BrandEntity, string> {

  BrandCod: string = "";
  Brand: BrandEntity = new BrandEntity();
  txtBrandCodreadonly: boolean = false;

  @ViewChild('txtBrandCod') txtBrandCod!: ElementRef<HTMLInputElement>;
  @ViewChild('txtBrandName') txtBrandName!: ElementRef<HTMLInputElement>;

  constructor(
    private brandService: BrandService,
    private router: Router,
    private toastrService: ToastrService
  ) {
    this.GetParamUrl(this.router);
    this.FindDataForm(this.BrandCod);
  }
  GetParamUrl(router: Router): void {
    let urlTree: any = router.parseUrl(this.router.url);
    this.BrandCod = (urlTree.queryParams['BrandCod']) ? urlTree.queryParams['BrandCod'] : "";
  }
  async FindDataForm(BrandCod: string): Promise<void> {
    const rpt = await this.brandService.FindDataForm(BrandCod);

    if (!rpt.ErrorStatus) {
      this.Brand = rpt.DataAdditional.find(e => e.Name === "brand")?.Data;

      setTimeout(() => { this.LoadingForm(this.Brand); }, 100);
    }
  }
  LoadingForm(Brand: BrandEntity): void {

    if (!Brand) return;
    this.txtBrandCodreadonly = true;

    this.txtBrandCod.nativeElement.value = Brand.BrandCod;
    this.txtBrandName.nativeElement.value = Brand.BrandName;

  }
  async Save(): Promise<void> {
    if (!this.Brand) this.Brand = new BrandEntity();

    this.Brand.BrandCod = this.txtBrandCod.nativeElement.value;
    this.Brand.BrandName = this.txtBrandName.nativeElement.value;

    if (!this.validate(this.Brand)) return;

    const rpt = await this.brandService.Save(this.Brand);

    if (!rpt.ErrorStatus) {
      this.toastrService.success("Operación realizada con exito.");

      this.router.navigate(['/enterprise/product/pages/listBrand']);
    }
  }

  validate(brand: BrandEntity) {
    try {
      ValidationHelper.validLengthString(brand.BrandCod, 10, "El codigo de marca solo puedo tener 10 caracteres");
      ValidationHelper.validateIsNotEmpty(brand.BrandCod, "Debe ingresar un codigo para la marca");

      ValidationHelper.validLengthString(brand.BrandName, 128, "El nombre de la marca solo puedo tener 128 caracteres");
      ValidationHelper.validateIsNotEmpty(brand.BrandName, "Debe ingresar un nombre para la marca");
      return true;
    } catch (e: any) {
      this.toastrService.error(e.message);
      return false;
    }
  }

  validateKeypress(event: KeyboardEvent, id: string) {

    try {
      if (id === "txtBrandCod") {
        ValidationHelper.isValidString(event.key.toString(), "Error", /[a-zA-Z0-9]/);
      }
    } catch (e: any) {
      event.preventDefault();
    }
  }

  ngOnInit(): void {

  }

}
