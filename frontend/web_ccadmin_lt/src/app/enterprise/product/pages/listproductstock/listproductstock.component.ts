import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { DataSesionService } from 'src/app/enterprise/compartido/service/datasesion.service';
import { ActionModalConfirmService } from 'src/app/enterprise/shared/interface/ActionModalConfirmService';
import { ActionTableService } from 'src/app/enterprise/shared/interface/ActionTableService';
import { DataTablaGeneticDto } from 'src/app/enterprise/shared/model/dto/DataTablaGeneticDto';
import { ResponsePageSearch } from 'src/app/enterprise/shared/model/dto/ResponsePageSearch';
import { SearchDto } from 'src/app/enterprise/shared/model/dto/SearchDto';
import { ProductInfoStockDto } from '../../model/dto/ProductInfoStockDto';
import { ProductInfoStockService } from '../../service/ProductInfoStockService';
import { StoreService } from 'src/app/enterprise/store/service/store.service';
import { StoreEntity } from 'src/app/enterprise/shared/model/entity/StoreEntity';

@Component({
  selector: 'app-listproductstock',
  templateUrl: './listproductstock.component.html'
})
export class ListproductstockComponent implements OnInit, ActionTableService<ProductInfoStockDto>, ActionModalConfirmService {

  @ViewChild('txtSearch') txtSearch!: ElementRef<HTMLInputElement>;

  dataTable: DataTablaGeneticDto<ProductInfoStockDto> = new DataTablaGeneticDto();
  currentStoreCod: string = '';
  selectedStoreCod: string = '';
  storeList: StoreEntity[] = [];

  constructor(
    private productInfoStockService: ProductInfoStockService,
    private dataSesionService: DataSesionService,
    private toastrService: ToastrService,
    private storeService: StoreService
  ) { }

  ngOnInit(): void {
    this.currentStoreCod = this.dataSesionService.getSessionStorageDto().StoreCod;
    this.selectedStoreCod = this.currentStoreCod;
    void this.initialize();
  }

  private async initialize(): Promise<void> {
    const response = await this.storeService.FindAllList();
    if (response.ErrorStatus) {
      this.toastrService.error(response.Message || 'No se pudo cargar la lista de locales');
    } else {
      this.storeList = (response.Data ?? []).filter((store: StoreEntity) => store.Status === 'A');
    }
    await this.findAll(1, '');
  }

  actionModal(_modalId: string): void {
  }

  filter(page: number): void {
    void this.findAll(page, this.txtSearch?.nativeElement.value || '');
  }

  async findAll(page: number, query: string): Promise<void> {
    const search = new SearchDto();
    search.Page = page;
    search.Query = query;
    search.StoreCod = this.selectedStoreCod;

    const response = await this.productInfoStockService.findAll(search);
    if (response.ErrorStatus) {
      this.toastrService.error(response.Message || 'No se pudo consultar el stock por zona');
      return;
    }

    this.loadingTable(response.Data);
  }

  loadingTable(response: ResponsePageSearch<ProductInfoStockDto>): void {
    const data = new DataTablaGeneticDto<ProductInfoStockDto>();
    data.init(
      [
        { Name: 'Producto', key: 'product', FunctionKey: item => this.productLabel(item) },
        { Name: 'Variante', key: 'variant', FunctionKey: item => item.productInfo.Variant },
        { Name: 'Local', key: 'store', FunctionKey: item => item.productInfo.StoreCod },
        {
          Name: 'Stock físico',
          key: 'physical',
          FunctionKey: item => item.productInfo.NumPhysicalStock,
          CellClassFunction: () => this.stockClass('physical')
        },
        {
          Name: 'Stock reservado',
          key: 'reserved',
          FunctionKey: item => item.productInfo.NumReservedStock,
          CellClassFunction: () => this.stockClass('reserved')
        },
        {
          Name: 'Stock no disponible',
          key: 'unavailable',
          FunctionKey: item => item.productInfo.NumUnavailableStock,
          CellClassFunction: () => this.stockClass('unavailable')
        },
        {
          Name: 'Stock total',
          key: 'total',
          FunctionKey: item => item.productInfo.NumTotalStock,
          CellClassFunction: () => this.stockClass('total')
        }
      ],
      { data: response },
      'Stock actual por zona'
    );
    this.dataTable = data;
  }

  getDataRow(_item: ProductInfoStockDto): void {
  }

  selectedStoreLabel(): string {
    if (!this.selectedStoreCod) {
      return 'Todos los locales';
    }
    const store = this.storeList.find(item => item.StoreCod === this.selectedStoreCod);
    return store ? `${store.StoreCod} - ${store.Name}` : this.selectedStoreCod;
  }

  private productLabel(item: ProductInfoStockDto): string {
    return item.product
      ? `${item.product.ProductCod} - ${item.product.ProductName}`
      : item.productInfo.ProductCod;
  }

  private stockClass(zone: 'physical' | 'reserved' | 'unavailable' | 'total'): string {
    const classes = {
      physical: 'badge badge-sm bgc-green-d1 text-white pb-1 px-3',
      reserved: 'badge badge-sm bgc-yellow-d2 text-dark pb-1 px-3',
      unavailable: 'badge badge-sm bgc-red-d1 text-white pb-1 px-3',
      total: 'badge badge-sm bgc-blue-d1 text-white pb-1 px-3'
    };
    return classes[zone];
  }
}
