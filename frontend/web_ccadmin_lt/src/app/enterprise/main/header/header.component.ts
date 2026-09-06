import { StoreService } from '../../store/service/store.service';
import { ToastrService } from 'ngx-toastr';
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

  constructor(
    private router: Router,
    private storeService: StoreService,
    private toastrService: ToastrService,
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
    try {
      const response = await this.storeService.FindById(this.storeCode);
      if (response.ErrorStatus) {
        this.toastrService.error(response.Message || 'No se pudo cargar el nombre de la tienda actual');
        return;
      }
      this.storeName = response.Data?.Name?.trim() || '';
    } catch {
      this.toastrService.error('No se pudo cargar el nombre de la tienda actual');
    }
  }

  Logout()
  {
    this.dataSesionService.ClearSession();
    this.router.navigate(['/login']);
  }

}
