import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { ActionTableService } from 'src/app/enterprise/shared/interface/ActionTableService';
import { ActionModalConfirmService } from 'src/app/enterprise/shared/interface/ActionModalConfirmService';
import { DataSesionService } from 'src/app/enterprise/compartido/service/datasesion.service';
import { DataTablaGeneticDto } from 'src/app/enterprise/shared/model/dto/DataTablaGeneticDto';
import { ResponsePageSearch } from 'src/app/enterprise/shared/model/dto/ResponsePageSearch';
import { SearchDto } from 'src/app/enterprise/shared/model/dto/SearchDto';
import { KardexDto } from '../../model/dto/KardexDto';
import { KardexZoneDto } from '../../model/dto/KardexZoneDto';
import { KardexZoneSearchDto } from '../../model/dto/KardexZoneSearchDto';
import { KardexService } from '../../service/KardexService';

type KardexView = 'total' | 'zone';

@Component({
  selector: 'app-listkardex',
  templateUrl: './listkardex.component.html'
})
export class ListkardexComponent implements OnInit, ActionTableService<any>, ActionModalConfirmService {

  @ViewChild('txtSearch') txtSearch!: ElementRef<HTMLInputElement>;

  viewMode: KardexView = 'total';
  zoneFilter: string = '';
  typeOperationFilter: string = '';
  dataTablaGenetic: DataTablaGeneticDto<KardexDto> = new DataTablaGeneticDto();
  dataTablaGeneticZone: DataTablaGeneticDto<KardexZoneDto> = new DataTablaGeneticDto();

  constructor(
    private kardexService: KardexService,
    private dataSesionService: DataSesionService
  ) {
  }

  ngOnInit(): void {
    this.findAll(1, '');
  }

  actionModal(_modalId: string): void {
  }

  filter(page: number): void {
    this.findAll(page, this.txtSearch.nativeElement.value);
  }

  loadingTable(response: ResponsePageSearch<any>): void {
    if (this.viewMode === 'zone') {
      this.loadingZoneTable(response as ResponsePageSearch<KardexZoneDto>);
      return;
    }
    this.loadingTotalTable(response as ResponsePageSearch<KardexDto>);
  }

  changeView(view: KardexView): void {
    if (this.viewMode === view) {
      return;
    }
    this.viewMode = view;
    this.findAll(1, this.txtSearch.nativeElement.value);
  }

  async findAll(page: number, query: string): Promise<void> {
    if (this.viewMode === 'zone') {
      await this.findAllZone(page, query);
      return;
    }

    const search: SearchDto = new SearchDto();
    search.Page = page;
    search.Query = query;
    search.StoreCod = this.storeCod();
    const response = await this.kardexService.FindAll(search);

    if (!response.ErrorStatus) {
      this.loadingTotalTable(response.Data);
    }
  }

  private async findAllZone(page: number, query: string): Promise<void> {
    const search: KardexZoneSearchDto = new KardexZoneSearchDto();
    search.Page = page;
    search.Query = query;
    search.StoreCod = this.storeCod();
    search.ZoneStockMoved = this.zoneFilter;
    search.TypeOperation = this.typeOperationFilter;
    const response = await this.kardexService.FindAllZone(search);

    if (!response.ErrorStatus) {
      this.loadingZoneTable(response.Data);
    }
  }

  private loadingTotalTable(response: ResponsePageSearch<KardexDto>): void {
    const data: DataTablaGeneticDto<KardexDto> = new DataTablaGeneticDto();

    data.init(
      [
        { Name: 'Id', key: 'id', FunctionKey: (item: KardexDto) => item.kardex.kardexID },
        { Name: 'Producto', key: 'product', FunctionKey: (item: KardexDto) => this.productLabel(item) },
        { Name: 'Stock anterior', key: 'before', FunctionKey: (item: KardexDto) => item.kardex.NumStockBefore },
        {
          Name: 'Movimiento',
          key: 'movement',
          FunctionKey: (item: KardexDto) => this.signedMovement(item.kardex.TypeOperation, item.kardex.NumStockMoved),
          CellClassFunction: (item: KardexDto) => this.movementClass(item.kardex.TypeOperation)
        },
        { Name: 'Stock resultante', key: 'after', FunctionKey: (item: KardexDto) => item.kardex.NumStockAfter },
        { Name: 'Lote', key: 'lot', FunctionKey: (item: KardexDto) => this.lotLabel(item.kardex.LotNumber) },
        { Name: 'Fecha venc.', key: 'expiration', FunctionKey: (item: KardexDto) => this.formatDateOnly(item.kardex.ExpirationDate) },
        { Name: 'Tipo de operación', key: 'type', FunctionKey: (item: KardexDto) => item.dataTypeOperation?.ConfigVal || item.kardex.TypeOperation },
        { Name: 'Cod. operación', key: 'operation', FunctionKey: (item: KardexDto) => item.kardex.OperationCod },
        { Name: 'Fecha', key: 'date', FunctionKey: (item: KardexDto) => item.kardex.CreationDate, IsDate: true }
      ],
      { data: response },
      'Movimientos de Kardex total'
    );

    this.dataTablaGenetic = data;
  }

  private loadingZoneTable(response: ResponsePageSearch<KardexZoneDto>): void {
    const data: DataTablaGeneticDto<KardexZoneDto> = new DataTablaGeneticDto();

    data.init(
      [
        { Name: 'Id', key: 'id', FunctionKey: (item: KardexZoneDto) => item.kardexZone.KardexZoneID },
        { Name: 'Producto', key: 'product', FunctionKey: (item: KardexZoneDto) => this.zoneProductLabel(item) },
        { Name: 'Almacén', key: 'warehouse', FunctionKey: (item: KardexZoneDto) => item.kardexZone.WarehouseCod },
        { Name: 'Zona', key: 'zone', FunctionKey: (item: KardexZoneDto) => this.zoneLabel(item.kardexZone.ZoneStockMoved) },
        { Name: 'Saldo anterior', key: 'before', FunctionKey: (item: KardexZoneDto) => item.kardexZone.NumZoneStockBefore },
        {
          Name: 'Movimiento',
          key: 'movement',
          FunctionKey: (item: KardexZoneDto) => this.signedMovement(item.kardexZone.TypeOperation, item.kardexZone.NumStockMoved),
          CellClassFunction: (item: KardexZoneDto) => this.movementClass(item.kardexZone.TypeOperation)
        },
        { Name: 'Saldo resultante', key: 'after', FunctionKey: (item: KardexZoneDto) => item.kardexZone.NumZoneStockAfter },
        { Name: 'Evento', key: 'event', FunctionKey: (item: KardexZoneDto) => item.kardexZone.MovementEvent },
        { Name: 'Documento origen', key: 'source', FunctionKey: (item: KardexZoneDto) => `${item.kardexZone.SourceTable} / ${item.kardexZone.OperationCod}` },
        { Name: 'Ítem', key: 'item', FunctionKey: (item: KardexZoneDto) => item.kardexZone.ItemNumber },
        { Name: 'Lote', key: 'lot', FunctionKey: (item: KardexZoneDto) => this.lotLabel(item.kardexZone.LotNumber) },
        { Name: 'Fecha', key: 'date', FunctionKey: (item: KardexZoneDto) => item.kardexZone.CreationDate, IsDate: true }
      ],
      { data: response },
      'Movimientos de Kardex por zonas'
    );

    this.dataTablaGeneticZone = data;
  }

  getDataRow(_item: any): void {
  }

  private productLabel(item: KardexDto): string {
    return item.product
      ? `${item.product.ProductCod} - ${item.product.ProductName}`
      : item.kardex.ProductCod;
  }

  private zoneProductLabel(item: KardexZoneDto): string {
    return item.product
      ? `${item.product.ProductCod} - ${item.product.ProductName}`
      : item.kardexZone.ProductCod;
  }

  private signedMovement(typeOperation: string, quantity: number): string {
    return `${typeOperation === 'R' ? '-' : '+'}${quantity}`;
  }

  private movementClass(typeOperation: string): string {
    if (typeOperation === 'S') {
      return 'badge badge-sm bgc-green-d1 text-white pb-1 px-25';
    }
    if (typeOperation === 'R') {
      return 'badge badge-sm bgc-red-d1 text-white pb-1 px-25';
    }
    return 'badge badge-sm bgc-secondary-l2 text-dark pb-1 px-25';
  }

  private zoneLabel(zone: string): string {
    const labels: { [key: string]: string } = {
      PHYSICAL: 'Stock físico',
      RESERVED: 'Stock reservado',
      UNAVAILABLE: 'Stock no disponible'
    };
    return labels[zone] || zone;
  }

  private lotLabel(lotNumber: string): string {
    return lotNumber && lotNumber.trim() ? lotNumber : 'SN';
  }

  private storeCod(): string {
    return this.dataSesionService.getSessionStorageDto().StoreCod;
  }

  private formatDateOnly(value: any): string {
    if (!value) {
      return '';
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return '';
    }
    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    return `${day}/${month}/${date.getFullYear()}`;
  }
}
