import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { LoginService } from '../service/login.service';
import { DataSesionService } from '../../compartido/service/datasesion.service';

@Component({
  selector: 'app-signin',
  templateUrl: './signin.component.html'
})
export class SigninComponent implements OnInit {

  @ViewChild('txt_usuario',{static: false}) txt_usuario!: ElementRef<HTMLInputElement>;
  @ViewChild('txt_password',{static: false}) txt_password!: ElementRef<HTMLInputElement>;

  constructor(
    private g_loginService : LoginService,
    private router: Router,
    private dataSesionService: DataSesionService
  ) { 

    if(this.dataSesionService.SessionExists())
    {
      this.router.navigate([
        this.dataSesionService.RequiresApplicationInitialization()
          ? '/enterprise/system/pages/applicationinitialization'
          : '/'
      ]);
    }

  }

  ngOnInit(): void {
  }



  IsSigningIn = false;

  async IniciarSesion() {
    if (this.IsSigningIn) return;
    this.IsSigningIn = true;
    try {

    await this.g_loginService.IniciarSesion(
        this.txt_usuario.nativeElement.value,
        this.txt_password.nativeElement.value
      );
    } finally { this.IsSigningIn = false; }
  }

}
