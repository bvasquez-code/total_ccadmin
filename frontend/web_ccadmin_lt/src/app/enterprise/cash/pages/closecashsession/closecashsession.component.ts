import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CloseRequestDto } from '../../model/dto/CloseRequestDto';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { ToastrService } from 'ngx-toastr';
import { CashsessionService } from '../../service/CashsessionService';
import { CurrentCashSessionDto } from '../../model/dto/CurrentCashSessionDto';

@Component({
  selector: 'app-closecashsession',
  templateUrl: './closecashsession.component.html'
})
export class ClosecashsessionComponent implements OnInit {

  req: CloseRequestDto = new CloseRequestDto();
  isReady: boolean = false;

  constructor(
    private cashSessionService: CashsessionService,
    private router: Router,
    private toastrService: ToastrService
  ) { }

  ngOnInit(): void {
    this.loadCurrentCashSession();
  }

  private async loadCurrentCashSession(): Promise<void> {
    const rpt: ResponseWsDto = await this.cashSessionService.findCurrent();
    if (rpt.ErrorStatus) return;

    const current: CurrentCashSessionDto = rpt.Data;
    if (!current.IsOpen || !current.CashSession) {
      this.router.navigate(["enterprise/cash/pages/opencashsession"]);
      return;
    }

    this.req.CashSessionID = current.CashSession.CashSessionID;
    this.isReady = true;
  }

  async close() {
    if (!this.isReady) return;

    const rpt: ResponseWsDto = await this.cashSessionService.close(this.req);
    if (!rpt.ErrorStatus) {
      this.toastrService.success("Caja cerrada");
      this.router.navigate(["enterprise/cash/pages/viewcashsession"], { queryParams: { CashSessionID: this.req.CashSessionID } });
    }
  }
}
