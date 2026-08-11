import { Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { ClientLoginDto } from '../../model/dto/ClientLoginDto';
import { ClientSessionService } from '../../service/client-session.service';

@Component({
  selector: 'app-client-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {
  public Login = new ClientLoginDto();
  public ShowPassword: boolean = false;
  public IsSubmitting: boolean = false;
  public ReturnUrl: string = '/';

  public constructor(
    private clientSessionService: ClientSessionService,
    private activatedRoute: ActivatedRoute,
    private router: Router,
    private toastrService: ToastrService
  ) {
    this.ReturnUrl = this.activatedRoute.snapshot.queryParamMap.get('returnUrl') || '/';
  }

  public async submit(): Promise<void> {
    this.Login.Email = this.Login.Email.trim().toLowerCase();
    if (!this.Login.Email || !this.Login.Password) {
      this.toastrService.warning('Ingresa tu correo y contraseña.');
      return;
    }

    this.IsSubmitting = true;
    const response = await this.clientSessionService.login(this.Login);
    this.IsSubmitting = false;

    if (response.ErrorStatus) {
      this.toastrService.error(response.Message || 'No pudimos iniciar sesión.');
      return;
    }

    await this.router.navigateByUrl(this.ReturnUrl);
    this.toastrService.success('Bienvenido a la tienda virtual.');
  }
}
