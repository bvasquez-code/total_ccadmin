import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { CloseRequestDto } from '../../model/dto/CloseRequestDto';
import { CurrentCashSessionDto } from '../../model/dto/CurrentCashSessionDto';
import { OpenRequestDto } from '../../model/dto/OpenRequestDto';
import { CashSessionEntity } from '../../model/entity/extends AuditTableEntity';
import { CashsessionService } from '../../service/CashsessionService';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';

@Component({
  selector: 'app-opencashsession',
  templateUrl: './opencashsession.component.html'
})
export class OpencashsessionComponent implements OnInit {

  openRequest: OpenRequestDto = new OpenRequestDto();
  closeRequest: CloseRequestDto = new CloseRequestDto();
  current: CurrentCashSessionDto = new CurrentCashSessionDto();
  isLoading: boolean = true;
  lastClosedCashSessionId: number = 0;

  constructor(
    private cashSessionService: CashsessionService,
    private router: Router,
    private toastrService: ToastrService
  ) { }

  ngOnInit(): void {
    this.loadCurrentCashSession();
  }

  get isOpen(): boolean {
    return this.current.IsOpen && this.current.CashSession !== null;
  }

  get activeCashSession(): CashSessionEntity | null {
    return this.current.CashSession;
  }

  private async loadCurrentCashSession(): Promise<void> {
    this.isLoading = true;
    const response: ResponseWsDto = await this.cashSessionService.findCurrent();
    if (!response.ErrorStatus) {
      this.current = response.Data;
    }
    this.isLoading = false;
  }

  async open(): Promise<void> {
    if (this.isLoading || !this.current.CashRegister) return;

    const response: ResponseWsDto = await this.cashSessionService.open(this.openRequest);
    if (!response.ErrorStatus) {
      this.toastrService.success('Caja aperturada');
      this.openRequest = new OpenRequestDto();
      await this.loadCurrentCashSession();
    }
  }

  onCashCountChange(): void {
    if (this.closeRequest.HasCashCount !== 'S') {
      this.closeRequest.CountedCashAmount = null;
      this.closeRequest.CountedOtherAmount = null;
    }
  }

  canClose(): boolean {
    return this.isOpen && !this.isLoading && (
      this.closeRequest.HasCashCount !== 'S'
      || (this.closeRequest.CountedCashAmount !== null
        && this.closeRequest.CountedOtherAmount !== null)
    );
  }

  async close(): Promise<void> {
    if (!this.canClose()) return;

    const response: ResponseWsDto = await this.cashSessionService.close(this.closeRequest);
    if (!response.ErrorStatus) {
      this.lastClosedCashSessionId = response.Data.CashSessionID;
      this.toastrService.success('Caja cerrada');
      this.closeRequest = new CloseRequestDto();
      await this.loadCurrentCashSession();
    }
  }

  viewSession(cashSessionId: number): void {
    this.router.navigate(
      ['enterprise/cash/pages/viewcashsession'],
      { queryParams: { CashSessionID: cashSessionId } }
    );
  }
}
