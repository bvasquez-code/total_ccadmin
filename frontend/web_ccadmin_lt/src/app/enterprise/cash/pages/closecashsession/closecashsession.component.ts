import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CloseRequestDto } from '../../model/dto/CloseRequestDto';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { ToastrService } from 'ngx-toastr';
import { CashsessionService } from '../../service/CashsessionService';

@Component({
  selector: 'app-closecashsession',
  templateUrl: './closecashsession.component.html'
})
export class ClosecashsessionComponent implements OnInit {

  req: CloseRequestDto = new CloseRequestDto();

  constructor(
    private cashSessionService: CashsessionService,
    private router: Router,
    private toastrService: ToastrService
  ) {
    let urlTree: any = this.router.parseUrl(this.router.url);
    this.req.CashSessionID = Number(urlTree.queryParams['CashSessionID'] ?? 0);
  }

  ngOnInit(): void { }

  async close() {
    const rpt: ResponseWsDto = await this.cashSessionService.close(this.req);
    if (!rpt.ErrorStatus) {
      this.toastrService.success("Caja cerrada");
      this.router.navigate(["enterprise/cash/pages/viewcashsession"], { queryParams: { CashSessionID: this.req.CashSessionID } });
    }
  }
}
