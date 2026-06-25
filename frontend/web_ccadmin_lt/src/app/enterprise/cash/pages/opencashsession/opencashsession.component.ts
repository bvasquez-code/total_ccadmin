import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { OpenRequestDto } from '../../model/dto/OpenRequestDto';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { DataSesionService } from 'src/app/enterprise/compartido/service/datasesion.service';
import { ToastrService } from 'ngx-toastr';
import { CashsessionService } from '../../service/CashsessionService';

@Component({
  selector: 'app-opencashsession',
  templateUrl: './opencashsession.component.html'
})
export class OpencashsessionComponent implements OnInit {

  req: OpenRequestDto = new OpenRequestDto();

  constructor(
    private cashSessionService: CashsessionService,
    private router: Router,
    private session: DataSesionService,
    private toastrService: ToastrService
  ) {
    let urlTree: any = this.router.parseUrl(this.router.url);
    this.req.RegisterCod = urlTree.queryParams['RegisterCod'] ?? "";
    this.req.StoreCod = urlTree.queryParams['StoreCod'] ?? this.session.getSessionStorageDto().StoreCod;
  }

  ngOnInit(): void { }

  async open() {
    const rpt: ResponseWsDto = await this.cashSessionService.open(this.req);
    if (!rpt.ErrorStatus) {
      this.toastrService.success("Caja aperturada");
      this.router.navigate(["enterprise/cash/pages/viewcashsession"], { queryParams: { CashSessionID: rpt.Data.CashSessionID } });
    }
  }
}
