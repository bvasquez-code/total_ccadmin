import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { DataSesionService } from '../../compartido/service/datasesion.service';

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css']
})
export class HeaderComponent implements OnInit {

  userNames: string = '';
  userCode: string = '';

  constructor(
    private router: Router,
    private dataSesionService: DataSesionService,
  ) { }

  ngOnInit(): void {
    const session = this.dataSesionService.getSessionStorageDto();
    this.userNames = session.Names?.trim() || 'Usuario';
    this.userCode = session.UserCod?.trim() || '';
  }

  Logout()
  {
    this.dataSesionService.ClearSession();
    this.router.navigate(['/login']);
  }

}
