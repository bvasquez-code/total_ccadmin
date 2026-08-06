import { AfterViewInit, Component, ElementRef, OnInit, ViewChild, Input, Output, EventEmitter } from '@angular/core';
import { ProductService } from '../../service/product.service';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { ProductEntity } from '../../model/entity/ProductEntity';
import { ProductRegisterDto } from '../../model/dto/ProductRegisterDto';
import { Router } from '@angular/router';
import { BrandEntity } from '../../model/entity/BrandEntity';
import { CategoryEntity } from '../../model/entity/CategoryEntity';
import { ToastrService } from 'ngx-toastr';
import { AppFileEntity } from 'src/app/enterprise/system/model/entity/AppFileEntity';
import { ProductPictureEntity } from '../../model/entity/ProductPictureEntity';
import { ValidationHelper } from 'src/app/enterprise/shared/helper/ValidationHelper';
import Swal from 'sweetalert2';
import { ProductBarcodeEntity } from '../../model/entity/ProductBarcodeEntity';
import { ProductUnitHelper } from 'src/app/enterprise/shared/helper/ProductUnitHelper';
import { DataSesionService } from 'src/app/enterprise/compartido/service/datasesion.service';
import { ProductConfigEntity } from '../../model/entity/ProductConfigEntity';

interface ProductUnitOption {
  Code: string;
  Name: string;
}

@Component({
  selector: 'app-createproduct',
  templateUrl: './createproduct.component.html'
})
export class CreateproductComponent implements OnInit, AfterViewInit {

  @Input() isModal: boolean = false;
  @Output() ProductCreated = new EventEmitter<ProductRegisterDto>();
  @Output() CancelModal = new EventEmitter<void>();

  ProductCod: string = "";
  ProductRegister: ProductRegisterDto = new ProductRegisterDto();
  BrandList: BrandEntity[] = [];
  CategoryList: CategoryEntity[] = [];
  readonly ProductUnitList: ProductUnitOption[] = [
    { Code: 'NIU', Name: 'Unidad' },
    { Code: 'KGM', Name: 'Kilogramo' },
    { Code: 'GRM', Name: 'Gramo' },
    { Code: 'LTR', Name: 'Litro' },
    { Code: 'MLT', Name: 'Mililitro' },
    { Code: 'MTR', Name: 'Metro' },
    { Code: 'MTK', Name: 'Metro cuadrado' },
    { Code: 'MTQ', Name: 'Metro cúbico' },
    { Code: 'BX', Name: 'Caja' },
    { Code: 'PK', Name: 'Paquete' },
    { Code: 'BG', Name: 'Bolsa' },
    { Code: 'BO', Name: 'Botella' },
    { Code: 'CA', Name: 'Lata' },
    { Code: 'DZN', Name: 'Docena' },
    { Code: 'SET', Name: 'Juego' },
    { Code: 'PR', Name: 'Par' },
    { Code: 'TNE', Name: 'Tonelada' },
    { Code: 'ZZ', Name: 'Servicio' }
  ];

  searchBrandTerm: string = '';
  searchCategoryTerm: string = '';
  searchProductUnitTerm: string = 'NIU - Unidad';
  showBrandDropdown: boolean = false;
  showCategoryDropdown: boolean = false;
  showProductUnitDropdown: boolean = false;
  showCreateBrand: boolean = false;
  showCreateCategory: boolean = false;

  lastKeypressTime: number = 0;
  inputBuffer: string = '';

  txtProductCodreadonly: boolean = false;
  isGeneratingProductCod: boolean = false;
  isSavingProduct: boolean = false;

  private readonly defaultMinStock: number = 50;
  private readonly defaultMaxStock: number = 100;

  @ViewChild('txtBarCode') txtBarCode!: ElementRef<HTMLInputElement>;
  @ViewChild('txtProductCod') txtProductCod!: ElementRef<HTMLInputElement>;
  @ViewChild('txtProductName') txtProductName!: ElementRef<HTMLInputElement>;
  @ViewChild('txtProductDesc') txtProductDesc!: ElementRef<HTMLTextAreaElement>;
  @ViewChild('cboCategoryCod') cboCategoryCod!: ElementRef<HTMLInputElement>;
  @ViewChild('cboBrandCod') cboBrandCod!: ElementRef<HTMLInputElement>;
  @ViewChild('txtNumPrice') txtNumPrice!: ElementRef<HTMLInputElement>;
  @ViewChild('txtVisibleUnitPrice') txtVisibleUnitPrice!: ElementRef<HTMLInputElement>;
  @ViewChild('txtProductUnitName') txtProductUnitName!: ElementRef<HTMLInputElement>;
  @ViewChild('txtProductUnitFactor') txtProductUnitFactor!: ElementRef<HTMLInputElement>;
  @ViewChild('btnCloseModalCreateBrand') btnCloseModalCreateBrand!: ElementRef<HTMLButtonElement>;
  @ViewChild('btnCloseModalCreateCategory') btnCloseModalCreateCategory!: ElementRef<HTMLButtonElement>;

  constructor(
    private productService: ProductService,
    private router: Router,
    private toastrService: ToastrService,
    private dataSesionService: DataSesionService
  ) {
    let urlTree: any = this.router.parseUrl(this.router.url);
    this.ProductCod = (urlTree.queryParams['ProductCod']) ? urlTree.queryParams['ProductCod'] : "";
    this.FindDataForm(this.ProductCod);
  }

  ngOnInit(): void {
  }

  ngAfterViewInit(): void {
    setTimeout(() => this.ensureUnitDefaults(), 0);
  }

  get IsEditMode(): boolean {
    return Boolean(this.ProductCod);
  }

  get CanCreateBrand(): boolean {
    return this.dataSesionService.PermissionExists('PR000006');
  }

  get CanCreateCategory(): boolean {
    return this.dataSesionService.PermissionExists('PR000007');
  }

  async FindDataForm(ProductCod: string) {
    const rpt: ResponseWsDto = await this.productService.FindDataForm(ProductCod);

    if (!rpt.ErrorStatus) {
      this.BrandList = rpt.DataAdditional.find(e => e.Name === "brandList")?.Data || [];
      this.CategoryList = rpt.DataAdditional.find(e => e.Name === "categoryList")?.Data || [];
      const productData = rpt.DataAdditional.find(e => e.Name === "product")?.Data;
      this.ProductRegister = this.buildProductRegister(productData);

      setTimeout(() => { this.loadingForm(this.ProductRegister); }, 100);

    }
  }

  private buildProductRegister(data: ProductRegisterDto | null | undefined): ProductRegisterDto {
    const productRegister = new ProductRegisterDto();

    if (!data) {
      return productRegister;
    }

    Object.assign(productRegister, data);
    productRegister.product = Object.assign(new ProductEntity(), data.product || {});
    productRegister.config = Object.assign(new ProductConfigEntity(), data.config || {});
    productRegister.productBarcode = Object.assign(new ProductBarcodeEntity(), data.productBarcode || {});
    productRegister.pictureList = data.pictureList || [];

    return productRegister;
  }

  loadingForm(ProductRegister: ProductRegisterDto) {
    if (!this.ProductRegister) return;

    this.txtProductCod.nativeElement.value = this.ProductRegister.product.ProductCod;
    this.txtProductName.nativeElement.value = this.ProductRegister.product.ProductName;
    this.txtProductDesc.nativeElement.value = this.ProductRegister.product.ProductDesc;
    this.cboBrandCod.nativeElement.value = this.ProductRegister.product.BrandCod;
    this.cboCategoryCod.nativeElement.value = this.ProductRegister.product.CategoryCod;

    const selectedBrand = this.BrandList.find(b => b.BrandCod === this.ProductRegister.product.BrandCod);
    if (selectedBrand) {
      this.searchBrandTerm = selectedBrand.BrandName;
    }

    const selectedCategory = this.CategoryList.find(c => c.CategoryCod === this.ProductRegister.product.CategoryCod);
    if (selectedCategory) {
      this.searchCategoryTerm = this.categoryVisibleLabel(selectedCategory);
      this.applyCategoryDigitalRule(selectedCategory);
    }

    this.ProductRegister.config.ProductCod = this.txtProductCod.nativeElement.value;
    this.ProductRegister.config.IsDigital = (this.ProductRegister.config.IsDigital || "N").trim().toUpperCase();

    if (!this.IsEditMode) {
      this.ProductRegister.config.NumMinStock = this.defaultMinStock;
      this.ProductRegister.config.NumMaxStock = this.defaultMaxStock;
      this.txtProductUnitName.nativeElement.value = this.ProductRegister.config.ProductUnitName || "NIU";
      this.searchProductUnitTerm = this.productUnitVisibleLabel(this.txtProductUnitName.nativeElement.value);
      this.txtProductUnitFactor.nativeElement.value = String(this.ProductRegister.config.ProductUnitFactor || 1);
      this.txtNumPrice.nativeElement.value = String(this.ProductRegister.config.NumPrice || 0);
      this.syncVisiblePriceFromInternal();
    }

    this.txtProductCodreadonly = this.IsEditMode;

    if (this.ProductRegister.productBarcode) {
      this.txtBarCode.nativeElement.value = this.ProductRegister.productBarcode.BarCode;
    }
  }

  async save() {
    if (this.isSavingProduct || this.isGeneratingProductCod) return;

    this.isSavingProduct = true;
    try {
      if (!this.ProductRegister) {
        this.ProductRegister = new ProductRegisterDto();
      }
      if (!this.IsEditMode && !this.txtProductCod.nativeElement.value.trim()) {
        const generated = await this.generateProductCod();
        if (!generated) return;
      }

      this.ProductRegister.product.ProductCod = this.txtProductCod.nativeElement.value.trim();
      this.ProductRegister.product.ProductName = this.txtProductName.nativeElement.value;
      this.ProductRegister.product.ProductDesc = this.txtProductDesc.nativeElement.value;
      this.ProductRegister.product.BrandCod = this.cboBrandCod.nativeElement.value;
      this.ProductRegister.product.CategoryCod = this.cboCategoryCod.nativeElement.value;

      this.ProductRegister.config.ProductCod = this.ProductRegister.product.ProductCod;
      this.applyCategoryDigitalRule(this.getSelectedCategory());
      this.ProductRegister.config.IsDigital = (this.ProductRegister.config.IsDigital || "N").trim().toUpperCase();

      if (!this.IsEditMode) {
        this.ProductRegister.config.NumPrice = Number(this.txtNumPrice.nativeElement.value);
        this.ProductRegister.config.NumMinStock = this.defaultMinStock;
        this.ProductRegister.config.NumMaxStock = this.defaultMaxStock;
        this.ProductRegister.config.ProductUnitName = this.txtProductUnitName.nativeElement.value || "NIU";
        this.ProductRegister.config.ProductUnitFactor = Number(this.txtProductUnitFactor.nativeElement.value || 1);
      }

      this.ProductRegister.config.IsDiscontable = "N";
      this.ProductRegister.config.DiscountType = "-";
      this.ProductRegister.config.NumDiscountMax = 0;
      this.ProductRegister.config.Version = "V.1";

      if (!this.ProductRegister.productBarcode) {
        this.ProductRegister.productBarcode = new ProductBarcodeEntity();
      }

      this.ProductRegister.productBarcode.ProductCod = this.ProductRegister.product.ProductCod;
      this.ProductRegister.productBarcode.BarCode = this.txtBarCode.nativeElement.value;
      this.ProductRegister.IsEditMode = this.IsEditMode;

      if (!this.validate(this.ProductRegister)) return;

      const rpt: ResponseWsDto = await this.productService.Save(this.ProductRegister);

      if (!rpt.ErrorStatus) {
        this.toastrService.success("Operación realizada con exito.");

        if (this.isModal) {
          this.ProductCreated.emit(this.ProductRegister);
        } else {
          this.router.navigate(['/enterprise/product/pages/listProduct']);
        }
      } else {
        this.toastrService.error(rpt.Message);
      }
    } finally {
      this.isSavingProduct = false;
    }
  }

  async generateProductCod(): Promise<boolean> {
    if (this.IsEditMode || this.isGeneratingProductCod) return false;

    this.isGeneratingProductCod = true;
    try {
      const rpt: ResponseWsDto = await this.productService.GenerateProductCode();

      if (!rpt.ErrorStatus) {
        const generatedProductCod = String(rpt.Data || '').trim();
        if (!generatedProductCod) {
          this.toastrService.error('No se pudo generar el código del producto.');
          return false;
        }
        this.txtProductCod.nativeElement.value = generatedProductCod;
        return true;
      } else {
        this.toastrService.error(rpt.Message);
        return false;
      }
    } finally {
      this.isGeneratingProductCod = false;
    }
  }

  cancel() {
    if (this.isModal) {
      this.CancelModal.emit();
    } else {
      this.router.navigate(['/enterprise/product/pages/listProduct']);
    }
  }

  ResponseResultFormAppFile(event: any) {

    const appFile: AppFileEntity = event;

    console.log(appFile);

    if (appFile) {

      if (!this.ProductRegister) {
        this.ProductRegister = new ProductRegisterDto();
      }

      let productPicture: ProductPictureEntity = new ProductPictureEntity();

      productPicture.FileCod = appFile.FileCod;
      productPicture.ProductCod = this.txtProductCod.nativeElement.value;
      productPicture.IsPrincipal = "N";
      productPicture.appFile = appFile;

      this.ProductRegister.pictureList.push(productPicture);

    }

  }

  setImagePrincipal(FileCod: string) {

    this.ProductRegister.pictureList.forEach(picture => {
      picture.IsPrincipal = 'N';
    });

    const fileImage = this.ProductRegister.pictureList.find(e => e.FileCod === FileCod);

    if (fileImage) {
      fileImage.IsPrincipal = "S";
    }

  }

  async deleteImage(productPicture: ProductPictureEntity) {

    Swal.fire({
      title: '¿Estás seguro?',
      text: 'Esta imagen se eliminará permanentemente',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#3085d6',
      cancelButtonColor: '#d33',
      confirmButtonText: 'Sí, eliminar',
      cancelButtonText: 'No, cancelar'
    }).then(async (result) => {
      if (result.isConfirmed) {
        const rpt: ResponseWsDto = await this.productService.DeletePicture(productPicture);
        if (!rpt.ErrorStatus) {
          this.FindDataForm(this.ProductCod);
        }
      }
    });
  }

  validate(productRegister: ProductRegisterDto): boolean {
    try {


      ValidationHelper.validLengthString(productRegister.product.ProductCod, 20, "El codigo de producto solo puedo tener 20 caracteres");
      ValidationHelper.validateIsNotEmpty(productRegister.product.ProductCod, "Debe ingresar un codigo para el producto");

      ValidationHelper.validLengthString(productRegister.product.ProductName, 128, "El nombre del producto solo puede tener 128 caracteres");
      ValidationHelper.validateIsNotEmpty(productRegister.product.ProductName, "Debe ingresar un nombre para el producto");

      ValidationHelper.validLengthString(productRegister.product.ProductDesc, 256, "La descripición del producto solo puede tener 256 caracteres");

      ValidationHelper.validateIsNotEmpty(productRegister.product.BrandCod, "Seleccione una marca");
      ValidationHelper.validateIsNotEmpty(productRegister.product.CategoryCod, "Seleccione una categoria");

      ValidationHelper.validateIsNotEmpty(productRegister.config.NumPrice, "Debe ingresar un precio para el producto");
      ValidationHelper.validNumber(productRegister.config.NumPrice, null, 0, "Precio no valido");
      ValidationHelper.validateIsNotEmpty(productRegister.config.ProductUnitName, "Debe ingresar la unidad de venta");
      ValidationHelper.validLengthString(productRegister.config.ProductUnitName, 32, "La unidad de venta solo puede tener 32 caracteres");
      ValidationHelper.validNumber(productRegister.config.ProductUnitFactor, null, 1, "Factor de operacion no valido");

      return true;
    } catch (e: any) {
      this.toastrService.error(e.message);
      return false;
    }
  }

  ensureUnitDefaults(): void {
    if (this.txtProductUnitName && !this.txtProductUnitName.nativeElement.value) {
      this.txtProductUnitName.nativeElement.value = "NIU";
    }
    if (this.txtProductUnitName) {
      this.searchProductUnitTerm = this.productUnitVisibleLabel(this.txtProductUnitName.nativeElement.value);
    }
    if (this.txtProductUnitFactor && !Number(this.txtProductUnitFactor.nativeElement.value || 0)) {
      this.txtProductUnitFactor.nativeElement.value = "1";
    }
    if (this.txtNumPrice && !this.txtNumPrice.nativeElement.value) {
      this.txtNumPrice.nativeElement.value = "0";
    }
    this.syncVisiblePriceFromInternal();
  }

  getProductUnitFactor(): number {
    return ProductUnitHelper.normalizeFactor(Number(this.txtProductUnitFactor?.nativeElement.value || 1));
  }

  getProductUnitName(): string {
    return this.txtProductUnitName?.nativeElement.value || "NIU";
  }

  isUnitConfigReady(): boolean {
    return Boolean(this.getProductUnitName()) && Number(this.txtProductUnitFactor?.nativeElement.value || 0) > 0;
  }

  onUnitConfigChange(): void {
    if (!this.txtProductUnitName.nativeElement.value) {
      this.txtProductUnitName.nativeElement.value = "NIU";
    }
    this.syncVisiblePriceFromInternal();
  }

  get filteredProductUnits(): ProductUnitOption[] {
    const query = this.searchProductUnitTerm.trim().toLowerCase();
    if (!query) return this.ProductUnitList;
    return this.ProductUnitList.filter(item =>
      item.Code.toLowerCase().includes(query)
      || item.Name.toLowerCase().includes(query)
      || this.productUnitOptionLabel(item).toLowerCase().includes(query)
    );
  }

  onProductUnitSearchChange(value: string): void {
    this.showProductUnitDropdown = true;
    const option = this.findProductUnitOption(value);
    this.txtProductUnitName.nativeElement.value = option
      ? option.Code
      : value.trim().toUpperCase();
  }

  selectProductUnit(option: ProductUnitOption): void {
    this.searchProductUnitTerm = this.productUnitOptionLabel(option);
    this.txtProductUnitName.nativeElement.value = option.Code;
    this.showProductUnitDropdown = false;
    this.onUnitConfigChange();
  }

  onProductUnitBlur(): void {
    setTimeout(() => {
      const option = this.findProductUnitOption(this.searchProductUnitTerm);
      if (option) {
        this.selectProductUnit(option);
        return;
      }

      const manualUnit = this.searchProductUnitTerm.trim().toUpperCase();
      this.txtProductUnitName.nativeElement.value = manualUnit || 'NIU';
      this.searchProductUnitTerm = manualUnit || this.productUnitVisibleLabel('NIU');
      this.showProductUnitDropdown = false;
      this.onUnitConfigChange();
    }, 200);
  }

  private findProductUnitOption(value: string): ProductUnitOption | undefined {
    const normalizedValue = (value || '').trim().toLowerCase();
    return this.ProductUnitList.find(item =>
      item.Code.toLowerCase() === normalizedValue
      || item.Name.toLowerCase() === normalizedValue
      || this.productUnitOptionLabel(item).toLowerCase() === normalizedValue
    );
  }

  private productUnitVisibleLabel(codeOrName: string): string {
    const option = this.findProductUnitOption(codeOrName);
    return option ? this.productUnitOptionLabel(option) : codeOrName;
  }

  private productUnitOptionLabel(option: ProductUnitOption): string {
    return `${option.Code} - ${option.Name}`;
  }

  syncVisiblePriceFromInternal(): void {
    if (!this.txtVisibleUnitPrice || !this.txtNumPrice || !this.isUnitConfigReady()) return;
    this.txtVisibleUnitPrice.nativeElement.value = String(
      ProductUnitHelper.toVisibleUnitPrice(Number(this.txtNumPrice.nativeElement.value || 0), this.getProductUnitFactor())
    );
  }

  syncInternalPriceFromVisible(): void {
    if (!this.txtVisibleUnitPrice || !this.txtNumPrice || !this.isUnitConfigReady()) return;
    this.txtNumPrice.nativeElement.value = String(
      ProductUnitHelper.toInternalUnitPrice(Number(this.txtVisibleUnitPrice.nativeElement.value || 0), this.getProductUnitFactor())
    );
  }

  validateKeypress(event: KeyboardEvent, id: string) {

    try {
      if (id === "txtProductCod") {
        ValidationHelper.isValidString(event.key.toString(), "Error", /[a-zA-Z0-9]/);
      }
    } catch (e: any) {
      event.preventDefault();
    }
  }

  ProductCodEnter(event: KeyboardEvent) {
    const now = Date.now();
    const timeDifference = now - this.lastKeypressTime;
    let IsBarcodeReaderInput: boolean = false;

    if (timeDifference < 50) {
      this.inputBuffer += event.key;
    } else {
      this.inputBuffer = event.key;
    }

    this.lastKeypressTime = now;

    if (event.key === 'Enter') {
      if (this.isBarcodeScannerInput()) {
        IsBarcodeReaderInput = true;
      } else {
        IsBarcodeReaderInput = false;
      }
      this.inputBuffer = '';
    }
  }

  isBarcodeScannerInput(): boolean {
    return this.inputBuffer.length > 5;
  }

  openCreateBrandModal(): void {
    if (!this.CanCreateBrand) return;
    this.showBrandDropdown = false;
    this.showCreateBrand = false;
    setTimeout(() => this.showCreateBrand = true, 0);
  }

  async handleBrandCreated(brand: BrandEntity): Promise<void> {
    this.btnCloseModalCreateBrand.nativeElement.click();
    this.showCreateBrand = false;
    await this.refreshProductLookups();
    const refreshedBrand = this.BrandList.find(item => item.BrandCod === brand.BrandCod) || brand;
    if (!this.BrandList.some(item => item.BrandCod === refreshedBrand.BrandCod)) {
      this.BrandList = [...this.BrandList, refreshedBrand];
    }
    this.selectBrand(refreshedBrand);
  }

  handleCancelCreateBrand(): void {
    this.btnCloseModalCreateBrand.nativeElement.click();
    this.showCreateBrand = false;
  }

  openCreateCategoryModal(): void {
    if (!this.CanCreateCategory) return;
    this.showCategoryDropdown = false;
    this.showCreateCategory = false;
    setTimeout(() => this.showCreateCategory = true, 0);
  }

  async handleCategoryCreated(category: CategoryEntity): Promise<void> {
    this.btnCloseModalCreateCategory.nativeElement.click();
    this.showCreateCategory = false;
    await this.refreshProductLookups();
    const refreshedCategory = this.CategoryList.find(item => item.CategoryCod === category.CategoryCod) || category;
    if (!this.CategoryList.some(item => item.CategoryCod === refreshedCategory.CategoryCod)) {
      this.CategoryList = [...this.CategoryList, refreshedCategory];
    }
    this.selectCategory(refreshedCategory);
  }

  handleCancelCreateCategory(): void {
    this.btnCloseModalCreateCategory.nativeElement.click();
    this.showCreateCategory = false;
  }

  private async refreshProductLookups(): Promise<void> {
    const rpt: ResponseWsDto = await this.productService.FindDataForm('');
    if (rpt.ErrorStatus) return;

    this.BrandList = rpt.DataAdditional.find(e => e.Name === 'brandList')?.Data || this.BrandList;
    this.CategoryList = rpt.DataAdditional.find(e => e.Name === 'categoryList')?.Data || this.CategoryList;
  }

  get filteredBrands() {
    if (!this.searchBrandTerm) return this.BrandList;
    return this.BrandList.filter(b => b.BrandName.toLowerCase().includes(this.searchBrandTerm.toLowerCase()));
  }

  get filteredCategories() {
    if (!this.searchCategoryTerm) return this.CategoryList;
    const searchTerm = this.searchCategoryTerm.toLowerCase();
    return this.CategoryList.filter(category =>
      this.categoryVisibleLabel(category).toLowerCase().includes(searchTerm)
    );
  }

  selectBrand(brand: BrandEntity | null) {
    if (brand) {
      this.cboBrandCod.nativeElement.value = brand.BrandCod;
      this.searchBrandTerm = brand.BrandName;
    } else {
      this.cboBrandCod.nativeElement.value = '';
      this.searchBrandTerm = '';
    }
    this.showBrandDropdown = false;
  }

  onBrandBlur() {
    setTimeout(() => {
      this.showBrandDropdown = false;
      const exists = this.BrandList.find(b => b.BrandName.toLowerCase() === this.searchBrandTerm?.toLowerCase());
      if (!exists) {
        this.cboBrandCod.nativeElement.value = '';
        this.searchBrandTerm = '';
      } else {
        this.cboBrandCod.nativeElement.value = exists.BrandCod;
        this.searchBrandTerm = exists.BrandName;
      }
    }, 200);
  }

  selectCategory(category: CategoryEntity | null) {
    if (category) {
      this.cboCategoryCod.nativeElement.value = category.CategoryCod;
      this.searchCategoryTerm = this.categoryVisibleLabel(category);
      this.applyCategoryDigitalRule(category);
    } else {
      this.cboCategoryCod.nativeElement.value = '';
      this.searchCategoryTerm = '';
    }
    this.showCategoryDropdown = false;
  }

  onCategoryBlur() {
    setTimeout(() => {
      this.showCategoryDropdown = false;
      const normalizedSearch = (this.searchCategoryTerm || '').trim().toLowerCase();
      const exists = this.CategoryList.find(category =>
        category.CategoryName.toLowerCase() === normalizedSearch
        || this.categoryVisibleLabel(category).toLowerCase() === normalizedSearch
      );
      if (!exists) {
        this.cboCategoryCod.nativeElement.value = '';
        this.searchCategoryTerm = '';
      } else {
        this.cboCategoryCod.nativeElement.value = exists.CategoryCod;
        this.searchCategoryTerm = this.categoryVisibleLabel(exists);
        this.applyCategoryDigitalRule(exists);
      }
    }, 200);
  }

  get isSelectedCategoryDigital(): boolean {
    return this.isDigitalCategory(this.getSelectedCategory());
  }

  categoryVisibleLabel(category: CategoryEntity): string {
    return `${category.CategoryName} (${this.isDigitalCategory(category) ? 'digital' : 'regular'})`;
  }

  private getSelectedCategory(): CategoryEntity | null {
    const categoryCod = this.cboCategoryCod?.nativeElement?.value || this.ProductRegister.product.CategoryCod;
    return this.CategoryList.find(category => category.CategoryCod === categoryCod) || null;
  }

  private isDigitalCategory(category: CategoryEntity | null): boolean {
    return (category?.IsDigital || 'N').trim().toUpperCase() === 'S';
  }

  private applyCategoryDigitalRule(category: CategoryEntity | null): void {
    if (this.isDigitalCategory(category)) {
      this.ProductRegister.config.IsDigital = 'S';
    }
  }

}
