import { CurrentStoreService } from '../../store/service/current-store.service';
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { DataSesionService } from '../../compartido/service/datasesion.service';

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css']
})
export class HeaderComponent implements OnInit {

  storeName: string = '';
  storeCode: string = '';
  userNames: string = '';
  userCode: string = '';

  isViewMegaLi : boolean = false;
  isViewTasksLi : boolean = false;
  isViewNotificationsLi : boolean = false;

  constructor(
    private router: Router,
    private currentStoreService: CurrentStoreService,
    private dataSesionService: DataSesionService,
  ) { }

  ngOnInit(): void {
    const session = this.dataSesionService.getSessionStorageDto();
    this.userNames = session.Names?.trim() || 'Usuario';
    this.userCode = session.UserCod?.trim() || '';
    this.storeCode = session.StoreCod?.trim() || '';
    if (this.storeCode) void this.loadCurrentStore();
  }

  private async loadCurrentStore(): Promise<void> {
    this.storeName = await this.currentStoreService.getCurrentStoreName();
  }

  Logout()
  {
    this.dataSesionService.ClearSession();
    this.router.navigate(['/login']);
  }

}
