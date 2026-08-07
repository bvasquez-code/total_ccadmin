import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { OpenRequestDto } from '../../model/dto/OpenRequestDto';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { ToastrService } from 'ngx-toastr';
import { CashsessionService } from '../../service/CashsessionService';
import { CurrentCashSessionDto } from '../../model/dto/CurrentCashSessionDto';

@Component({
  selector: 'app-opencashsession',
  templateUrl: './opencashsession.component.html'
})
export class OpencashsessionComponent implements OnInit {

  req: OpenRequestDto = new OpenRequestDto();
  isReady: boolean = false;

  constructor(
    private cashSessionService: CashsessionService,
    private router: Router,
    private toastrService: ToastrService
  ) { }

  ngOnInit(): void {
    this.loadCurrentCashRegister();
  }

  private async loadCurrentCashRegister(): Promise<void> {
    const rpt: ResponseWsDto = await this.cashSessionService.findCurrent();
    if (rpt.ErrorStatus) return;

    const current: CurrentCashSessionDto = rpt.Data;
    if (current.IsOpen && current.CashSession) {
      this.router.navigate(["enterprise/cash/pages/closecashsession"]);
      return;
    }

    if (!current.CashRegister) return;

    this.req.RegisterCod = current.CashRegister.RegisterCod;
    this.req.StoreCod = current.CashRegister.StoreCod;
    this.isReady = true;
  }

  async open() {
    if (!this.isReady) return;

    const rpt: ResponseWsDto = await this.cashSessionService.open(this.req);
    if (!rpt.ErrorStatus) {
      this.toastrService.success("Caja aperturada");
      this.router.navigate(["enterprise/cash/pages/viewcashsession"], { queryParams: { CashSessionID: rpt.Data.CashSessionID } });
    }
  }
}
