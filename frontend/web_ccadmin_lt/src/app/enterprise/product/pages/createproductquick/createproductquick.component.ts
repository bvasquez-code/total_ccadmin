import {
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
  ViewChild
} from '@angular/core';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { DataSesionService } from 'src/app/enterprise/compartido/service/datasesion.service';
import { AlertService } from 'src/app/enterprise/shared/service/AlertService';
import { ValidationHelper } from 'src/app/enterprise/shared/helper/ValidationHelper';
import { AppFileDto } from 'src/app/enterprise/system/model/dto/AppFileDto';
import { AppFileEntity } from 'src/app/enterprise/system/model/entity/AppFileEntity';
import { AppFileService } from 'src/app/enterprise/system/service/AppFileService';
import { ProductImageAnalysisDto } from '../../model/dto/ProductImageAnalysisDto';
import { ProductRegisterDto } from '../../model/dto/ProductRegisterDto';
import { BrandEntity } from '../../model/entity/BrandEntity';
import { CategoryEntity } from '../../model/entity/CategoryEntity';
import { ProductBarcodeEntity } from '../../model/entity/ProductBarcodeEntity';
import { ProductEntity } from '../../model/entity/ProductEntity';
import { ProductPictureEntity } from '../../model/entity/ProductPictureEntity';
import { ProductQuickCreateService } from '../../service/product-quick-create.service';
import { ProductService } from '../../service/product.service';

type MasterValidationStatus =
  'pending' | 'valid' | 'missing' | 'similar' | 'created';
type ProductPhotoType = 'front' | 'side' | 'barcode';

interface ProductPhotoState {
  blob: Blob | null;
  previewUrl: string;
  uploadedAppFile: AppFileEntity | null;
}

@Component({
  selector: 'app-createproductquick',
  templateUrl: './createproductquick.component.html',
  styleUrls: ['./createproductquick.component.css']
})
export class CreateProductQuickComponent implements OnInit, OnDestroy {
  @Input() InitializationMode: boolean = false;
  @Output() ConfigurationCompleted: EventEmitter<string> =
    new EventEmitter<string>();
  @ViewChild('frontPhotoInput') frontPhotoInput?: ElementRef<HTMLInputElement>;
  @ViewChild('sidePhotoInput') sidePhotoInput?: ElementRef<HTMLInputElement>;
  @ViewChild('barcodePhotoInput') barcodePhotoInput?: ElementRef<HTMLInputElement>;
  @ViewChild('brandSuggestionInput') brandSuggestionInput?: ElementRef<HTMLInputElement>;
  @ViewChild('categorySuggestionInput') categorySuggestionInput?: ElementRef<HTMLInputElement>;
  @ViewChild('closeBrandModal') closeBrandModal?: ElementRef<HTMLButtonElement>;
  @ViewChild('closeCategoryModal') closeCategoryModal?: ElementRef<HTMLButtonElement>;

  readonly photoOrder: ProductPhotoType[] = ['front', 'side', 'barcode'];
  photos: Record<ProductPhotoType, ProductPhotoState> = {
    front: { blob: null, previewUrl: '', uploadedAppFile: null },
    side: { blob: null, previewUrl: '', uploadedAppFile: null },
    barcode: { blob: null, previewUrl: '', uploadedAppFile: null }
  };

  processingBarcode = false;
  barcode = '';
  verifiedBarcode = '';
  barcodeVerified = false;
  existingProduct: ProductEntity | null = null;

  analyzing = false;
  formVisible = false;
  saving = false;
  loadingLookups = false;

  productCod = '';
  productName = '';
  productDescription = '';
  numPrice: number | null = null;
  productNameSuggestions: string[] = [];

  brandList: BrandEntity[] = [];
  categoryList: CategoryEntity[] = [];
  brandSuggested = '';
  categorySuggested = '';
  selectedBrandCod = '';
  selectedCategoryCod = '';
  brandStatus: MasterValidationStatus = 'pending';
  categoryStatus: MasterValidationStatus = 'pending';
  similarBrands: BrandEntity[] = [];
  similarCategories: CategoryEntity[] = [];
  showCreateBrand = false;
  showCreateCategory = false;

  constructor(
    private productService: ProductService,
    private productQuickCreateService: ProductQuickCreateService,
    private appFileService: AppFileService,
    private dataSesionService: DataSesionService,
    private alertService: AlertService,
    private toastr: ToastrService,
    private router: Router
  ) {}

  ngOnInit(): void {
    void this.loadLookups();
  }

  ngOnDestroy(): void {
    this.releaseAllPhotoPreviews();
  }

  get canCreateBrand(): boolean {
    return this.dataSesionService.PermissionExists('PR000006');
  }

  get canCreateCategory(): boolean {
    return this.dataSesionService.PermissionExists('PR000007');
  }

  get hasAllPhotos(): boolean {
    return this.photoOrder.every(type => Boolean(this.photos[type].blob));
  }

  onBarcodeChanged(): void {
    if (this.clean(this.barcode) !== this.verifiedBarcode) {
      this.barcodeVerified = false;
      this.existingProduct = null;
      this.verifiedBarcode = '';
    }
  }

  async verifyBarcode(): Promise<void> {
    const barcode = this.clean(this.barcode);
    if (!barcode) {
      this.toastr.warning('Ingrese el código de barras manualmente o con la pistola láser');
      return;
    }
    if (barcode.length > 20) {
      this.toastr.error('El código de barras admite hasta 20 caracteres');
      return;
    }

    this.processingBarcode = true;
    this.barcodeVerified = false;
    this.existingProduct = null;
    try {
      const response = await this.productService.FindRegisteredByBarCode(barcode);
      if (response.ErrorStatus) {
        this.toastr.error(response.Message);
        return;
      }
      if (response.Data) {
        this.existingProduct = Object.assign(new ProductEntity(), response.Data);
        this.verifiedBarcode = barcode;
        this.toastr.warning('El código de barras ya pertenece a un producto registrado');
        return;
      }

      this.barcode = barcode;
      this.verifiedBarcode = barcode;
      this.barcodeVerified = true;
      this.toastr.success('Código de barras disponible. Ya puede confirmar el producto.');
    } finally {
      this.processingBarcode = false;
    }
  }

  async onPhotoSelected(event: Event, type: ProductPhotoType): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.item(0);
    input.value = '';
    if (!file) return;
    if (!file.type.startsWith('image/')) {
      this.toastr.error('Seleccione un archivo de imagen');
      return;
    }
    if (file.size > 12 * 1024 * 1024) {
      this.toastr.error('La fotografía original no puede superar 12 MB');
      return;
    }

    try {
      const compressedImage = await this.resizeImage(file);
      this.releasePhotoPreview(type);
      this.photos[type].blob = compressedImage;
      this.photos[type].previewUrl = await this.blobToDataUrl(compressedImage);
      this.photos[type].uploadedAppFile = null;
      if (this.formVisible) {
        this.resetAnalysisDraft();
        this.toastr.info('Fotografía actualizada. Analice nuevamente las tres imágenes.');
      }
    } catch (error) {
      this.toastr.error('No fue posible procesar la fotografía seleccionada');
    }
  }

  retakePhoto(type: ProductPhotoType): void {
    this.photoInputFor(type)?.nativeElement.click();
  }

  async analyzePhotos(): Promise<void> {
    if (!this.hasAllPhotos || this.analyzing) {
      this.toastr.warning('Debe tomar las fotografías frontal, lateral y del código de barras');
      return;
    }

    const frontImage = this.photos.front.blob as Blob;
    const sideImage = this.photos.side.blob as Blob;
    const barcodeImage = this.photos.barcode.blob as Blob;
    this.resetAnalysisDraft();
    this.analyzing = true;
    try {
      const response = await this.productQuickCreateService.analyzeImages(
        frontImage, sideImage, barcodeImage
      );
      if (response.ErrorStatus) {
        this.formVisible = true;
        this.toastr.warning(
          response.Message || 'No se pudo analizar la imagen. Continúe manualmente.'
        );
        return;
      }
      this.applyAnalysis(response.Data as ProductImageAnalysisDto);
      this.formVisible = true;
      this.toastr.success(
        'Datos sugeridos. Valide la información y revise el código de barras al final.'
      );
    } catch (error) {
      this.formVisible = true;
      this.toastr.warning('Gemini no está disponible. Continúe manualmente.');
    } finally {
      this.analyzing = false;
    }
  }

  continueWithoutAi(): void {
    if (!this.hasAllPhotos) {
      this.toastr.warning('Debe tomar las tres fotografías antes de continuar');
      return;
    }
    this.resetAnalysisDraft();
    this.formVisible = true;
  }

  selectSuggestedName(name: string): void {
    this.productName = name;
  }

  resetBrandValidation(): void {
    this.brandStatus = 'pending';
    this.selectedBrandCod = '';
    this.similarBrands = [];
  }

  resetCategoryValidation(): void {
    this.categoryStatus = 'pending';
    this.selectedCategoryCod = '';
    this.similarCategories = [];
  }

  validateBrand(): void {
    const suggestion = this.clean(this.brandSuggested);
    const exactMatches = this.brandList.filter(brand =>
      this.masterMatches(suggestion, brand.BrandCod, brand.BrandName)
    );
    if (exactMatches.length === 1) {
      this.selectBrand(exactMatches[0]);
      return;
    }
    this.similarBrands = exactMatches.length > 1
      ? exactMatches : this.findSimilarBrands(suggestion);
    this.selectedBrandCod = '';
    this.brandStatus = this.similarBrands.length > 0 ? 'similar' : 'missing';
  }

  validateCategory(): void {
    const suggestion = this.clean(this.categorySuggested);
    const exactMatches = this.categoryList.filter(category =>
      this.masterMatches(
        suggestion, category.CategoryCod, category.CategoryName
      )
    );
    if (exactMatches.length === 1) {
      this.selectCategory(exactMatches[0]);
      return;
    }
    this.similarCategories = exactMatches.length > 1
      ? exactMatches : this.findSimilarCategories(suggestion);
    this.selectedCategoryCod = '';
    this.categoryStatus = this.similarCategories.length > 0
      ? 'similar' : 'missing';
  }

  selectBrand(brand: BrandEntity, created = false): void {
    this.selectedBrandCod = brand.BrandCod;
    this.brandStatus = created ? 'created' : 'valid';
    this.similarBrands = [];
  }

  selectCategory(category: CategoryEntity, created = false): void {
    this.selectedCategoryCod = category.CategoryCod;
    this.categoryStatus = created ? 'created' : 'valid';
    this.similarCategories = [];
  }

  selectBrandByCode(code: string): void {
    const brand = this.brandList.find(item => item.BrandCod === code);
    if (brand) this.selectBrand(brand);
  }

  selectCategoryByCode(code: string): void {
    const category = this.categoryList.find(item => item.CategoryCod === code);
    if (category) this.selectCategory(category);
  }

  modifyBrandSuggestion(): void {
    this.brandSuggestionInput?.nativeElement.focus();
  }

  modifyCategorySuggestion(): void {
    this.categorySuggestionInput?.nativeElement.focus();
  }

  openCreateBrandModal(): void {
    if (!this.canCreateBrand) {
      this.toastr.warning('No cuenta con permiso para registrar marcas');
      return;
    }
    this.showCreateBrand = false;
    setTimeout(() => this.showCreateBrand = true, 0);
  }

  openCreateCategoryModal(): void {
    if (!this.canCreateCategory) {
      this.toastr.warning('No cuenta con permiso para registrar categorías');
      return;
    }
    this.showCreateCategory = false;
    setTimeout(() => this.showCreateCategory = true, 0);
  }

  handleBrandCreated(brand: BrandEntity): void {
    this.closeBrandModal?.nativeElement.click();
    this.showCreateBrand = false;
    if (!this.brandList.some(item => item.BrandCod === brand.BrandCod)) {
      this.brandList = [...this.brandList, brand];
    }
    this.selectBrand(brand, true);
  }

  handleCategoryCreated(category: CategoryEntity): void {
    this.closeCategoryModal?.nativeElement.click();
    this.showCreateCategory = false;
    if (!this.categoryList.some(item => item.CategoryCod === category.CategoryCod)) {
      this.categoryList = [...this.categoryList, category];
    }
    this.selectCategory(category, true);
  }

  cancelBrandModal(): void {
    this.closeBrandModal?.nativeElement.click();
    this.showCreateBrand = false;
  }

  cancelCategoryModal(): void {
    this.closeCategoryModal?.nativeElement.click();
    this.showCreateCategory = false;
  }

  brandStatusLabel(): string {
    return this.masterStatusLabel(this.brandStatus, 'Marca');
  }

  categoryStatusLabel(): string {
    return this.masterStatusLabel(this.categoryStatus, 'Categoría');
  }

  statusClass(status: MasterValidationStatus): string {
    if (status === 'valid' || status === 'created') return 'badge-success';
    if (status === 'similar') return 'badge-warning';
    if (status === 'missing') return 'badge-danger';
    return 'badge-secondary';
  }

  selectedBrandName(): string {
    return this.brandList.find(item => item.BrandCod === this.selectedBrandCod)
      ?.BrandName ?? '';
  }

  selectedCategoryName(): string {
    return this.categoryList.find(item => item.CategoryCod === this.selectedCategoryCod)
      ?.CategoryName ?? '';
  }

  async save(): Promise<void> {
    if (this.saving || !this.validateDraft()) return;

    const confirmation = await this.alertService.waring(
      `Se creará el producto "${this.clean(this.productName)}" con el código de barras ${this.verifiedBarcode}.`,
      'Confirmar creación rápida'
    );
    if (!confirmation.isConfirmed) return;

    this.saving = true;
    try {
      if (!this.clean(this.productCod)) {
        const codeResponse = await this.productService.GenerateProductCode();
        if (codeResponse.ErrorStatus || !this.clean(codeResponse.Data)) {
          this.toastr.error(
            codeResponse.Message || 'No se pudo generar el código del producto'
          );
          return;
        }
        this.productCod = this.clean(codeResponse.Data);
      }

      if (!await this.uploadProductPhotos()) return;

      const response = await this.productService.Save(this.buildProductRegister());
      if (response.ErrorStatus) {
        this.toastr.error(response.Message);
        return;
      }
      this.toastr.success('Producto creado correctamente');
      if (this.InitializationMode) {
        this.ConfigurationCompleted.emit(this.productCod);
      }
      const stockConfirmation = await this.alertService.waring(
        'El producto ya fue creado. ¿Desea registrar su stock inicial ahora?',
        'Registrar stock inicial'
      );
      if (stockConfirmation.isConfirmed) {
        await this.router.navigate(
          ['/enterprise/inventory/pages/createquickstockentry'],
          {
            queryParams: {
              productCod: this.productCod
            }
          }
        );
        return;
      }
      await this.router.navigate(['/enterprise/product/pages/listProduct']);
    } catch (error) {
      this.toastr.error('No fue posible crear el producto');
    } finally {
      this.saving = false;
    }
  }

  private async loadLookups(): Promise<void> {
    this.loadingLookups = true;
    try {
      const response = await this.productService.FindDataFormMassive();
      if (response.ErrorStatus) {
        this.toastr.error(response.Message);
        return;
      }
      this.brandList = response.DataAdditional
        .find(item => item.Name === 'brandList')?.Data ?? [];
      this.categoryList = response.DataAdditional
        .find(item => item.Name === 'categoryList')?.Data ?? [];
    } finally {
      this.loadingLookups = false;
    }
  }

  private applyAnalysis(analysis: ProductImageAnalysisDto): void {
    const names = Array.isArray(analysis?.ProductNameList)
      ? analysis.ProductNameList.map(name => this.clean(name)).filter(Boolean)
      : [];
    this.productNameSuggestions = names;
    this.productName = names[0] ?? this.productName;
    this.productDescription = this.clean(analysis?.ProductDesc)
      || this.productDescription;
    const suggestedPrice = Number(analysis?.NumPrice || 0);
    if (suggestedPrice > 0) this.numPrice = suggestedPrice;

    this.brandSuggested = this.clean(analysis?.BrandInput)
      || this.clean(analysis?.BrandCod);
    this.categorySuggested = this.clean(analysis?.CategoryInput)
      || this.clean(analysis?.CategoryCod);
    this.barcode = this.clean(analysis?.Barcode).replace(/\D/g, '').slice(0, 20);
    this.verifiedBarcode = '';
    this.barcodeVerified = false;
    this.existingProduct = null;
    this.resetBrandValidation();
    this.resetCategoryValidation();
  }

  private findSimilarBrands(suggestion: string): BrandEntity[] {
    return this.brandList.filter(brand =>
      this.isSimilar(suggestion, brand.BrandName)
      || this.isSimilar(suggestion, brand.BrandCod)
    ).slice(0, 5);
  }

  private findSimilarCategories(suggestion: string): CategoryEntity[] {
    return this.categoryList.filter(category =>
      this.isSimilar(suggestion, category.CategoryName)
      || this.isSimilar(suggestion, category.CategoryCod)
    ).slice(0, 5);
  }

  private masterMatches(input: string, code: string, name: string): boolean {
    const normalizedInput = this.normalizeMasterText(input);
    return normalizedInput !== '' && (
      normalizedInput === this.normalizeMasterText(code)
      || normalizedInput === this.normalizeMasterText(name)
    );
  }

  private isSimilar(left: string, right: string): boolean {
    const normalizedLeft = this.normalizeMasterText(left);
    const normalizedRight = this.normalizeMasterText(right);
    if (!normalizedLeft || !normalizedRight) return false;
    if (normalizedLeft.includes(normalizedRight)
      || normalizedRight.includes(normalizedLeft)) return true;
    const longest = Math.max(normalizedLeft.length, normalizedRight.length);
    return this.levenshtein(normalizedLeft, normalizedRight) / longest <= 0.35;
  }

  private normalizeMasterText(value: string): string {
    return this.clean(value)
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase()
      .replace(/[^a-z0-9]/g, '');
  }

  private levenshtein(left: string, right: string): number {
    const distances = Array.from({ length: right.length + 1 }, (_, i) => i);
    for (let leftIndex = 1; leftIndex <= left.length; leftIndex++) {
      let previous = distances[0];
      distances[0] = leftIndex;
      for (let rightIndex = 1; rightIndex <= right.length; rightIndex++) {
        const current = distances[rightIndex];
        distances[rightIndex] = left[leftIndex - 1] === right[rightIndex - 1]
          ? previous
          : Math.min(previous, distances[rightIndex], distances[rightIndex - 1]) + 1;
        previous = current;
      }
    }
    return distances[right.length];
  }

  private masterStatusLabel(
    status: MasterValidationStatus,
    masterName: string
  ): string {
    if (status === 'valid') return `${masterName} validada`;
    if (status === 'created') return 'Registrada y validada';
    if (status === 'missing') return `${masterName} no registrada`;
    if (status === 'similar') return 'Existen posibles coincidencias';
    return 'Pendiente de validar';
  }

  private validateDraft(): boolean {
    if (!this.hasAllPhotos) {
      this.toastr.error('Debe registrar las tres fotografías del producto');
      return false;
    }
    if (!this.barcodeVerified
      || this.clean(this.barcode) !== this.verifiedBarcode) {
      this.toastr.error('Revise y valide el código de barras como último paso');
      return false;
    }
    try {
      ValidationHelper.validateIsNotEmpty(
        this.verifiedBarcode, 'Debe verificar el código de barras'
      );
      ValidationHelper.validLengthString(
        this.verifiedBarcode, 20, 'El código de barras admite hasta 20 caracteres'
      );
      const code = this.clean(this.productCod);
      if (code) {
        ValidationHelper.validLengthString(
          code, 20, 'El código de producto admite hasta 20 caracteres'
        );
        ValidationHelper.isValidString(
          code, 'El código de producto debe ser alfanumérico', /^[a-zA-Z0-9]*$/
        );
      }
      ValidationHelper.validateIsNotEmpty(
        this.productName, 'Debe ingresar un nombre para el producto'
      );
      ValidationHelper.validLengthString(
        this.productName, 128, 'El nombre admite hasta 128 caracteres'
      );
      ValidationHelper.validLengthString(
        this.productDescription, 256, 'La descripción admite hasta 256 caracteres'
      );
      ValidationHelper.validateIsNotEmpty(
        this.selectedBrandCod, 'Debe validar o seleccionar una marca'
      );
      ValidationHelper.validateIsNotEmpty(
        this.selectedCategoryCod, 'Debe validar o seleccionar una categoría'
      );
      ValidationHelper.validateIsNotEmpty(
        this.numPrice, 'Debe ingresar el precio'
      );
      ValidationHelper.validNumber(
        this.numPrice, null, 0, 'El precio no es válido'
      );
      return true;
    } catch (error: any) {
      this.toastr.error(error.message);
      return false;
    }
  }

  private buildProductRegister(): ProductRegisterDto {
    const request = new ProductRegisterDto();
    request.product.ProductCod = this.clean(this.productCod);
    request.product.ProductName = this.clean(this.productName);
    request.product.ProductDesc = this.clean(this.productDescription);
    request.product.BrandCod = this.selectedBrandCod;
    request.product.CategoryCod = this.selectedCategoryCod;

    request.config.ProductCod = request.product.ProductCod;
    request.config.NumPrice = Number(this.numPrice);
    request.config.NumMinStock = 50;
    request.config.NumMaxStock = 100;
    request.config.ProductUnitName = 'NIU';
    request.config.ProductUnitFactor = 1;
    request.config.IsDigital = this.selectedCategory()?.IsDigital === 'S' ? 'S' : 'N';
    request.config.IsDiscontable = 'N';
    request.config.DiscountType = '-';
    request.config.NumDiscountMax = 0;
    request.config.Version = 'V.1';

    request.productBarcode = new ProductBarcodeEntity();
    request.productBarcode.ProductCod = request.product.ProductCod;
    request.productBarcode.BarCode = this.verifiedBarcode;
    request.IsEditMode = false;

    request.pictureList = [];
    this.photoOrder.forEach(type => {
      const uploadedAppFile = this.photos[type].uploadedAppFile;
      if (!uploadedAppFile) return;
      const picture = new ProductPictureEntity();
      picture.ProductCod = request.product.ProductCod;
      picture.FileCod = uploadedAppFile.FileCod;
      picture.IsPrincipal = type === 'front' ? 'S' : 'N';
      picture.appFile = uploadedAppFile;
      request.pictureList.push(picture);
    });
    return request;
  }

  private selectedCategory(): CategoryEntity | undefined {
    return this.categoryList.find(
      category => category.CategoryCod === this.selectedCategoryCod
    );
  }

  private async uploadProductPhotos(): Promise<boolean> {
    for (const type of this.photoOrder) {
      const photo = this.photos[type];
      if (!photo.blob) {
        this.toastr.error('Falta una fotografía obligatoria del producto');
        return false;
      }
      if (!photo.uploadedAppFile) {
        const uploadedAppFile = await this.uploadProductPhoto(photo.blob);
        if (!uploadedAppFile) return false;
        photo.uploadedAppFile = uploadedAppFile;
      }
    }
    return true;
  }

  private async uploadProductPhoto(image: Blob): Promise<AppFileEntity | null> {
    const dataUrl = await this.blobToDataUrl(image);
    const request = new AppFileDto();
    request.base64 = dataUrl;
    request.extension = 'jpg';
    request.type = 'data:image/jpeg;base64,';
    request.groupTypeFile = 1;
    const response = await this.appFileService.Save(request);
    if (response.ErrorStatus) {
      this.toastr.error(response.Message || 'No fue posible guardar la fotografía');
      return null;
    }
    return response.Data as AppFileEntity;
  }

  private blobToDataUrl(blob: Blob): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(String(reader.result || ''));
      reader.onerror = () => reject(reader.error);
      reader.readAsDataURL(blob);
    });
  }

  private resizeImage(file: File): Promise<Blob> {
    return new Promise((resolve, reject) => {
      const sourceUrl = URL.createObjectURL(file);
      const image = new Image();
      image.onload = () => {
        const scale = Math.min(1, 1280 / Math.max(image.width, image.height));
        const canvas = document.createElement('canvas');
        canvas.width = Math.max(1, Math.round(image.width * scale));
        canvas.height = Math.max(1, Math.round(image.height * scale));
        const context = canvas.getContext('2d');
        if (!context) {
          URL.revokeObjectURL(sourceUrl);
          reject(new Error('Canvas no disponible'));
          return;
        }
        context.drawImage(image, 0, 0, canvas.width, canvas.height);
        canvas.toBlob(blob => {
          URL.revokeObjectURL(sourceUrl);
          if (blob) resolve(blob);
          else reject(new Error('No fue posible comprimir la imagen'));
        }, 'image/jpeg', 0.82);
      };
      image.onerror = () => {
        URL.revokeObjectURL(sourceUrl);
        reject(new Error('Imagen no válida'));
      };
      image.src = sourceUrl;
    });
  }

  private photoInputFor(
    type: ProductPhotoType
  ): ElementRef<HTMLInputElement> | undefined {
    if (type === 'front') return this.frontPhotoInput;
    if (type === 'side') return this.sidePhotoInput;
    return this.barcodePhotoInput;
  }

  private resetAnalysisDraft(): void {
    this.formVisible = false;
    this.barcode = '';
    this.verifiedBarcode = '';
    this.barcodeVerified = false;
    this.existingProduct = null;
    this.productCod = '';
    this.productName = '';
    this.productDescription = '';
    this.numPrice = null;
    this.productNameSuggestions = [];
    this.brandSuggested = '';
    this.categorySuggested = '';
    this.selectedBrandCod = '';
    this.selectedCategoryCod = '';
    this.brandStatus = 'pending';
    this.categoryStatus = 'pending';
    this.similarBrands = [];
    this.similarCategories = [];
  }

  private releasePhotoPreview(type: ProductPhotoType): void {
    const previewUrl = this.photos[type].previewUrl;
    if (previewUrl.startsWith('blob:')) URL.revokeObjectURL(previewUrl);
    this.photos[type].previewUrl = '';
  }

  private releaseAllPhotoPreviews(): void {
    this.photoOrder.forEach(type => this.releasePhotoPreview(type));
  }

  private clean(value: unknown): string {
    return value === null || value === undefined ? '' : String(value).trim();
  }
}
