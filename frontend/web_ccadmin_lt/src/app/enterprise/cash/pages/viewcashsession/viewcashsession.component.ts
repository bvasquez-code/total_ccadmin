import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CashSessionEntity } from '../../model/entity/extends AuditTableEntity';
import { CashsessionService } from '../../service/CashsessionService';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';

@Component({
  selector: 'app-viewcashsession',
  templateUrl: './viewcashsession.component.html'
})
export class ViewcashsessionComponent implements OnInit {

  cashSession: CashSessionEntity | null = null;
  isLoading: boolean = true;

  constructor(
    private cashSessionService: CashsessionService,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.loadCashSession();
  }

  private async loadCashSession(): Promise<void> {
    const urlTree = this.router.parseUrl(this.router.url);
    const cashSessionId = Number(urlTree.queryParams['CashSessionID'] ?? 0);
    let response: ResponseWsDto;

    if (cashSessionId > 0) {
      response = await this.cashSessionService.findById(cashSessionId);
      if (!response.ErrorStatus) this.cashSession = response.Data;
    } else {
      response = await this.cashSessionService.findCurrent();
      if (!response.ErrorStatus) this.cashSession = response.Data.CashSession;
    }
    this.isLoading = false;
  }

  goClose(): void {
    this.router.navigate(["enterprise/cash/pages/opencashsession"]);
  }
}
