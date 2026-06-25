import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { PucharseService } from '../../service/PucharseService';
import { PucharseDetailsDto } from '../../model/dto/PucharseDetailsDto';
import { StoreEntity } from 'src/app/enterprise/shared/model/entity/StoreEntity';
import { WarehouseEntity } from 'src/app/enterprise/shared/model/entity/WarehouseEntity';
import { PucharsePrintService } from '../../service/PucharsePrintService';

@Component({
  selector: 'app-viewpucharse',
  templateUrl: './viewpucharse.component.html'
})
export class ViewpucharseComponent {

  PucharseCod: string = '';
  PucharseDetails: PucharseDetailsDto = new PucharseDetailsDto();
  Store: StoreEntity = new StoreEntity();
  WarehouseList: WarehouseEntity[] = [];

  constructor(
    private pucharseService: PucharseService,
    private router: Router,
    private pucharsePrintService: PucharsePrintService
  ) {
    let urlTree: any = this.router.parseUrl(this.router.url);
    this.PucharseCod = urlTree.queryParams['PucharseCod'] ?? '';
    this.FindDataForm(this.PucharseCod);
  }

  async FindDataForm(PucharseCod: string): Promise<void> {
    const rpt = await this.pucharseService.FindDataForm(PucharseCod);

    if (!rpt.ErrorStatus) {
      this.PucharseDetails = rpt.DataAdditional.find(e => e.Name === 'PucharseDetails')?.Data;
      this.Store = rpt.DataAdditional.find(e => e.Name === 'Store')?.Data;
      this.WarehouseList = rpt.DataAdditional.find(e => e.Name === 'WarehouseList')?.Data;
    }
  }

  getStatusLabel(status: string): string {
    const map: { [key: string]: string } = {
      F: 'Finalizado',
      P: 'Pendiente'
    };
    return map[status] ?? status;
  }

  getStatusClass(status: string): string {
    const map: { [key: string]: string } = {
      F: 'badge badge-sm bgc-info-d1 text-white pb-1 px-25',
      P: 'badge badge-sm bgc-red-d1 text-white pb-1 px-25'
    };
    return map[status] ?? 'badge badge-sm bgc-secondary-l2 text-dark pb-1 px-25';
  }

  getKardexLabel(value: string): string {
    return value === 'S' ? 'Recibido' : 'Pendiente';
  }

  getKardexClass(value: string): string {
    return value === 'S'
      ? 'badge badge-sm bgc-green-d1 text-white pb-1 px-25'
      : 'badge badge-sm bgc-orange-d1 text-white pb-1 px-25';
  }

  getLotNumber(value: string): string {
    return value && value.trim() ? value : 'SN';
  }

  get NumTotalPrice(): number {
    return this.PucharseDetails?.Headboard?.NumTotalPrice ?? 0;
  }

  print(): void {
    this.pucharsePrintService.printReception(this.PucharseDetails, this.PucharseDetails.DetailList || [], this.Store, this.WarehouseList);
  }

  goBack(): void {
    this.router.navigate(['/enterprise/pucharse/pages/listpucharse']);
  }
}
