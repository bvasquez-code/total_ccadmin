import { Component, OnInit } from '@angular/core';
import { DataSesionService } from '../../../compartido/service/datasesion.service';

@Component({
  selector: 'app-my-profile',
  templateUrl: './myprofile.component.html'
})
export class MyProfileComponent implements OnInit {
  userNames = '';
  userCode = '';
  email = '';
  storeCode = '';

  constructor(private dataSesionService: DataSesionService) {
  }

  ngOnInit(): void {
    const session = this.dataSesionService.getSessionStorageDto();
    this.userNames = session.Names?.trim() || '';
    this.userCode = session.UserCod?.trim() || '';
    this.email = session.Email?.trim() || '';
    this.storeCode = session.StoreCod?.trim() || '';
  }
}
