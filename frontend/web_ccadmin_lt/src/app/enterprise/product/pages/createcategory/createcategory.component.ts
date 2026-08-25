import { AfterViewInit, Component, ElementRef, EventEmitter, Input, OnInit, Output, ViewChild } from '@angular/core';
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
export class CreatecategoryComponent implements OnInit, AfterViewInit, IRegisterForm<CategoryEntity, string> {

  @Input() isModal: boolean = false;
  @Input() productSelectionMode: boolean = false;
  @Input() initialName: string = '';
  @Output() CategoryCreated = new EventEmitter<CategoryEntity>();
  @Output() CancelModal = new EventEmitter<void>();

  CategoryCod: string = "";
  Category: CategoryEntity = new CategoryEntity();
  CategoryDadList: CategoryEntity[] = [];
  txtCategoryCodReadOnly: boolean = false;
  cboCategoryDadCodvisibility: boolean = false;
  isGeneratingCategoryCod: boolean = false;
  isSavingCategory: boolean = false;

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

  get IsEditMode(): boolean {
    return Boolean(this.CategoryCod);
  }

  async Save(): Promise<void> {
    if (this.isSavingCategory || this.isGeneratingCategoryCod) return;

    this.isSavingCategory = true;
    try {
      if (!this.Category) this.Category = new CategoryEntity();
      if (!this.IsEditMode && !this.txtCategoryCod.nativeElement.value.trim()) {
        const generated = await this.generateCategoryCod();
        if (!generated) return;
      }

      this.Category.CategoryCod = this.txtCategoryCod.nativeElement.value.trim();
      this.Category.CategoryName = this.txtCategoryName.nativeElement.value;
      this.Category.CategoryDadCod = this.cboCategoryDadCod.nativeElement.value;
      this.Category.IsDigital = this.cboIsDigital.nativeElement.value;
      this.Category.IsCategoryDad = this.cboIsCategoryDad.nativeElement.value;

      if (!this.validate(this.Category)) return;

      const rpt = await this.categoryService.Save(this.Category);
      if (!rpt.ErrorStatus) {
        this.Category = rpt.Data || this.Category;
        this.toastrService.success("Operación realizada con exito.");

        if (this.isModal) {
          this.CategoryCreated.emit(this.Category);
        } else {
          this.router.navigate(['/enterprise/product/pages/listCategory']);
        }
      } else {
        this.toastrService.error(rpt.Message);
      }
    } finally {
      this.isSavingCategory = false;
    }
  }

  async generateCategoryCod(): Promise<boolean> {
    if (this.IsEditMode || this.isGeneratingCategoryCod) return false;

    this.isGeneratingCategoryCod = true;
    try {
      const rpt = await this.categoryService.GenerateCategoryCode();
      if (rpt.ErrorStatus) {
        this.toastrService.error(rpt.Message);
        return false;
      }
      const generatedCategoryCod = String(rpt.Data || '').trim();
      if (!generatedCategoryCod) {
        this.toastrService.error('No se pudo generar el código de la categoría.');
        return false;
      }
      this.txtCategoryCod.nativeElement.value = generatedCategoryCod;
      return true;
    } finally {
      this.isGeneratingCategoryCod = false;
    }
  }

  ngOnInit(): void {
  }

  ngAfterViewInit(): void {
    if (!this.IsEditMode && this.productSelectionMode) {
      this.cboIsCategoryDad.nativeElement.value = 'N';
      this.IsCategoryDad();
    }
    if (!this.IsEditMode && this.initialName) {
      this.txtCategoryName.nativeElement.value = this.initialName;
    }
  }

  cancel(): void {
    if (this.isModal) {
      this.CancelModal.emit();
    } else {
      this.router.navigate(['/enterprise/product/pages/listCategory']);
    }
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
