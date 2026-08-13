import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { DataSesionService } from 'src/app/enterprise/compartido/service/datasesion.service';
import { ProductInfoDto } from '../../model/dto/ProductInfoDto';
import { ProductRegisterDto } from '../../model/dto/ProductRegisterDto';
import { ProductService } from '../../service/product.service';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';

@Component({
  selector: 'app-viewproduct',
  templateUrl: './viewproduct.component.html',
  styleUrls: ['./viewproduct.component.css']
})
export class ViewproductComponent implements OnInit {

  ProductCod: string = '';
  ProductInfo: ProductInfoDto = new ProductInfoDto();
  ProductRegister: ProductRegisterDto = new ProductRegisterDto();
  BrandList: any[] = [];
  CategoryList: any[] = [];
  ImageRoutes: string[] = [];
  SelectedImageRoute: string = '';
  IsLoading: boolean = true;
  HasLoadError: boolean = false;

  constructor(
    private activatedRoute: ActivatedRoute,
    private productService: ProductService,
    private session: DataSesionService,
    private toastrService: ToastrService
  ) {
  }

  async ngOnInit(): Promise<void> {
    this.ProductCod = (this.activatedRoute.snapshot.queryParamMap.get('ProductCod') || '').trim();
    if (!this.ProductCod) {
      this.IsLoading = false;
      this.HasLoadError = true;
      this.toastrService.error('No se indicó el producto que desea visualizar.');
      return;
    }

    await this.loadProduct();
  }

  selectImage(route: string): void {
    this.SelectedImageRoute = route;
  }

  closeWindow(): void {
    window.close();
  }

  isDigital(): boolean {
    return (this.ProductInfo?.Config?.IsDigital || 'N').trim().toUpperCase() === 'S';
  }

  getVisiblePrice(): number {
    return this.toMoney(
      Number(this.ProductInfo?.Config?.NumPrice || 0) * this.getUnitFactor()
    );
  }

  getVisibleStock(): number {
    const stock = (this.ProductInfo?.InfoList || [])
      .reduce((total, item) => total + Number(item.NumPhysicalStock || 0), 0);
    return Math.max(0, Math.floor(stock / this.getUnitFactor()));
  }

  getUnitName(): string {
    return this.ProductInfo?.Config?.ProductUnitName || 'NIU';
  }

  getBrandName(): string {
    const BrandCod = this.ProductInfo?.Product?.BrandCod;
    return this.BrandList.find(item => item.BrandCod === BrandCod)?.BrandName || BrandCod || 'Sin marca';
  }

  getCategoryName(): string {
    const CategoryCod = this.ProductInfo?.Product?.CategoryCod;
    return this.CategoryList.find(item => item.CategoryCod === CategoryCod)?.CategoryName
      || CategoryCod || 'Sin categoría';
  }

  private async loadProduct(): Promise<void> {
    this.IsLoading = true;
    this.HasLoadError = false;

    const StoreCod = this.session.getSessionStorageDto().StoreCod;
    const [detailResponse, formResponse]: ResponseWsDto[] = await Promise.all([
      this.productService.findDetailById(this.ProductCod, StoreCod),
      this.productService.FindDataForm(this.ProductCod)
    ]);

    if (detailResponse.ErrorStatus || !detailResponse.Data?.Product) {
      this.IsLoading = false;
      this.HasLoadError = true;
      this.toastrService.error(detailResponse.Message || 'No se pudo cargar el detalle del producto.');
      return;
    }

    this.ProductInfo = detailResponse.Data;

    if (!formResponse.ErrorStatus) {
      this.ProductRegister = formResponse.DataAdditional.find(item => item.Name === 'product')?.Data
        || new ProductRegisterDto();
      this.BrandList = formResponse.DataAdditional.find(item => item.Name === 'brandList')?.Data || [];
      this.CategoryList = formResponse.DataAdditional.find(item => item.Name === 'categoryList')?.Data || [];
    } else {
      this.toastrService.warning('El producto se cargó, pero no fue posible recuperar toda su galería.');
    }

    this.ImageRoutes = this.buildImageRoutes();
    this.SelectedImageRoute = this.ImageRoutes[0] || '/assets/image/avatar/NO_IMAGEN.png';
    this.IsLoading = false;
  }

  private buildImageRoutes(): string[] {
    return (this.ProductRegister.pictureList || [])
      .slice()
      .sort((left, right) => (right.IsPrincipal === 'S' ? 1 : 0) - (left.IsPrincipal === 'S' ? 1 : 0))
      .map(picture => picture.appFile?.Route?.trim())
      .filter((route): route is string => !!route)
      .filter((route, index, routes) => routes.indexOf(route) === index);
  }

  private getUnitFactor(): number {
    return Math.max(1, Number(this.ProductInfo?.Config?.ProductUnitFactor || 1));
  }

  private toMoney(value: number): number {
    return Math.round(Number(value || 0) * 100) / 100;
  }
}
