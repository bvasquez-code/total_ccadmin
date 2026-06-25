import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { IRegisterForm } from 'src/app/enterprise/shared/interface/IRegisterForm';
import { CategoryEntity } from '../../model/entity/CategoryEntity';
import { Router } from '@angular/router';
import { CategoryService } from '../../service/category.service';
import { ToastrService } from 'ngx-toastr';
import { ValidationHelper } from 'src/app/enterprise/shared/helper/ValidationHelper';

@Component({
  selector: 'app-createcategory',
  templateUrl: './createcategory.component.html'
})
export class CreatecategoryComponent implements OnInit, IRegisterForm<CategoryEntity, string> {

  CategoryCod: string = "";
  Category: CategoryEntity = new CategoryEntity();
  CategoryDadList: CategoryEntity[] = [];
  txtCategoryCodReadOnly: boolean = false;
  cboCategoryDadCodvisibility: boolean = false;

  @ViewChild('txtCategoryCod') txtCategoryCod!: ElementRef<HTMLInputElement>;
  @ViewChild('txtCategoryName') txtCategoryName!: ElementRef<HTMLInputElement>;
  @ViewChild('cboCategoryDadCod') cboCategoryDadCod!: ElementRef<HTMLSelectElement>;
  @ViewChild('cboIsDigital') cboIsDigital!: ElementRef<HTMLSelectElement>;
  @ViewChild('cboIsCategoryDad') cboIsCategoryDad!: ElementRef<HTMLSelectElement>;

  constructor(
    private categoryService: CategoryService,
    private router: Router,
    private toastrService: ToastrService
  ) {
    this.GetParamUrl(this.router);
    this.FindDataForm(this.CategoryCod);
  }

  GetParamUrl(router: Router): void {
    let urlTree: any = router.parseUrl(this.router.url);
    this.CategoryCod = (urlTree.queryParams['CategoryCod']) ? urlTree.queryParams['CategoryCod'] : "";
  }
  async FindDataForm(CategoryCod: string): Promise<void> {
    const rpt = await this.categoryService.FindDataForm(this.CategoryCod);

    if (!rpt.ErrorStatus) {
      this.Category = rpt.DataAdditional.find(e => e.Name === "category")?.Data;
      this.CategoryDadList = rpt.DataAdditional.find(e => e.Name === "categoryDadList")?.Data;

      setTimeout(() => { this.LoadingForm(this.Category); }, 100);
    }
  }
  LoadingForm(Category: CategoryEntity): void {
    if (!Category) return;
    this.txtCategoryCodReadOnly = true;

    this.txtCategoryCod.nativeElement.value = Category.CategoryCod;
    this.txtCategoryName.nativeElement.value = Category.CategoryName;
    this.cboCategoryDadCod.nativeElement.value = Category.CategoryDadCod;
    this.cboIsDigital.nativeElement.value = Category.IsDigital;
    this.cboIsCategoryDad.nativeElement.value = Category.IsCategoryDad;
    this.IsCategoryDad();
  }
  async Save(): Promise<void> {

    if (!this.Category) this.Category = new CategoryEntity();

    this.Category.CategoryCod = this.txtCategoryCod.nativeElement.value;
    this.Category.CategoryName = this.txtCategoryName.nativeElement.value;
    this.Category.CategoryDadCod = this.cboCategoryDadCod.nativeElement.value;
    this.Category.IsDigital = this.cboIsDigital.nativeElement.value;
    this.Category.IsCategoryDad = this.cboIsCategoryDad.nativeElement.value;

    if (!this.validate(this.Category)) return;

    const rpt = await this.categoryService.Save(this.Category);
    if (!rpt.ErrorStatus) {
      this.toastrService.success("Operación realizada con exito.");

      this.router.navigate(['/enterprise/product/pages/listCategory']);
    }
  }
  ngOnInit(): void {
    throw new Error('Method not implemented.');
  }

  IsCategoryDad() {
    this.cboCategoryDadCodvisibility = (this.cboIsCategoryDad.nativeElement.value === "S") ? false : true;
  }

  validate(Category: CategoryEntity) {
    try {
      ValidationHelper.validLengthString(Category.CategoryCod, 10, "El codigo de cateogia solo puedo tener 10 caracteres");
      ValidationHelper.validateIsNotEmpty(Category.CategoryCod, "Codigo de cateogia no puede ser vacio");

      ValidationHelper.validLengthString(Category.CategoryName, 128, "El nombre de cateogia solo puedo tener 128 caracteres");
      ValidationHelper.validateIsNotEmpty(Category.CategoryName, "Nombre de cateogia no puede ser vacio");

      if (Category.IsCategoryDad === "N") {
        ValidationHelper.validateIsNotEmpty(Category.CategoryDadCod, "Selecciona el codigo de categoria padre");
      }

      return true;
    } catch (e: any) {
      this.toastrService.error(e.message);
      return false;
    }
  }

  validateKeypress(event: KeyboardEvent, id: string) {
    try {
      if (id === "txtCategoryCod") {
        ValidationHelper.isValidString(event.key.toString(), "Error", /[a-zA-Z0-9]/);
      }
    } catch (e: any) {
      event.preventDefault();
    }
  }

}
