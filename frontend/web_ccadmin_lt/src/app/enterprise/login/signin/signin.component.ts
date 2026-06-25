import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { RespuestaWsDto } from '../../compartido/entity/RespuestaWsDto';
import { LoginService } from '../service/login.service';

@Component({
  selector: 'app-signin',
  templateUrl: './signin.component.html'
})
export class SigninComponent implements OnInit {

  @ViewChild('txt_usuario',{static: false}) txt_usuario!: ElementRef<HTMLInputElement>;
  @ViewChild('txt_password',{static: false}) txt_password!: ElementRef<HTMLInputElement>;

  ExisteSesion : boolean = ( sessionStorage.getItem('Token') ) ? true : false;

  constructor(
    private g_loginService : LoginService,
    private router: Router
  ) { 

    if(this.ExisteSesion)
    {
      this.router.navigate(['/']);    
    }

  }

  ngOnInit(): void {
  }



  async IniciarSesion() {

    const response : RespuestaWsDto = this.g_loginService.IniciarSesion(
        this.txt_usuario.nativeElement.value,
        this.txt_password.nativeElement.value
      );    
  }

}
