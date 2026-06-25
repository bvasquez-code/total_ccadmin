import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { CashRegisterEntity } from '../../model/entity/CashRegisterEntity';
import { ToastrService } from 'ngx-toastr';
import { CashregisterService } from '../../service/CashregisterService';
import { SessionStorageDto } from 'src/app/enterprise/compartido/entity/SessionStorageDto';
import { DataSesionService } from 'src/app/enterprise/compartido/service/datasesion.service';
import { StoreEntity } from 'src/app/enterprise/shared/model/entity/StoreEntity';


@Component({
  selector: 'app-createcashregister',
  templateUrl: './createcashregister.component.html'
})
export class CreatecashregisterComponent implements OnInit {

  RegisterCod: string = "";
  cashRegister: CashRegisterEntity = new CashRegisterEntity();
  store: StoreEntity = new StoreEntity();

  constructor(
    private cashRegisterService: CashregisterService,
    private router: Router,
    private toastrService: ToastrService,
    private dataSesionService: DataSesionService
  ) {
    let urlTree: any = this.router.parseUrl(this.router.url);
    this.RegisterCod = urlTree.queryParams['RegisterCod'] ?? "";
  }

  ngOnInit(): void {
    this.findDataForm(this.RegisterCod);
    this.cashRegister.StoreCod = this.getStoreCod();
  }

  async findById(RegisterCod: string) {
    const rpt: ResponseWsDto = await this.cashRegisterService.findById(RegisterCod);
    if (!rpt.ErrorStatus) this.cashRegister = rpt.Data;
  }

  async findDataForm(RegisterCod: string) {
    const rpt: ResponseWsDto = await this.cashRegisterService.findDataForm(RegisterCod);
    if (!rpt.ErrorStatus) {
      this.cashRegister = rpt.DataAdditional.find(e => e.Name == "cashRegister")?.Data;
      this.store = rpt.DataAdditional.find(e => e.Name == "store")?.Data;
    }
  }

  async save() {
    const rpt: ResponseWsDto = await this.cashRegisterService.save(this.cashRegister);
    if (!rpt.ErrorStatus) {
      this.toastrService.success("Caja guardada");
      this.router.navigate(["enterprise/cash/pages/listcashregister"]);
    }
  }

  getStoreCod(): string {
    return this.dataSesionService.getSessionStorageDto().StoreCod;
  }
}
