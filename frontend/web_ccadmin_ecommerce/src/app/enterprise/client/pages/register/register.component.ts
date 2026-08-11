import { Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { ClientRegisterDto } from '../../model/dto/ClientRegisterDto';
import { ClientSessionService } from '../../service/client-session.service';

@Component({
  selector: 'app-client-register',
  templateUrl: './register.component.html',
  styleUrls: ['../login/login.component.css', './register.component.css']
})
export class RegisterComponent {
  public Register = new ClientRegisterDto();
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
    this.normalize();
    if (!this.validate()) return;

    this.IsSubmitting = true;
    const response = await this.clientSessionService.register(this.Register);
    this.IsSubmitting = false;

    if (response.ErrorStatus) {
      this.toastrService.error(response.Message || 'No pudimos crear tu cuenta.');
      return;
    }

    await this.router.navigateByUrl(this.ReturnUrl);
    this.toastrService.success('Tu cuenta fue creada correctamente.');
  }

  public documentMaxLength(): number {
    return this.Register.DocumentType === '01' ? 8 : 16;
  }

  private normalize(): void {
    this.Register.DocumentNumber = this.Register.DocumentNumber.trim().toUpperCase();
    this.Register.Names = this.Register.Names.trim();
    this.Register.LastNames = this.Register.LastNames.trim();
    this.Register.Phone = this.Register.Phone.trim();
    this.Register.Email = this.Register.Email.trim().toLowerCase();
  }

  private validate(): boolean {
    if (!this.Register.DocumentNumber || !this.Register.Names || !this.Register.LastNames
      || !this.Register.Phone || !this.Register.Email || !this.Register.Password) {
      this.toastrService.warning('Completa todos los campos del registro.');
      return false;
    }
    if (this.Register.DocumentType === '01' && !/^\d{8}$/.test(this.Register.DocumentNumber)) {
      this.toastrService.warning('El DNI debe contener exactamente 8 números.');
      return false;
    }
    if (this.Register.DocumentType === '04'
      && !/^[a-zA-Z0-9]{9,16}$/.test(this.Register.DocumentNumber)) {
      this.toastrService.warning('El carnet de extranjería debe contener entre 9 y 16 caracteres.');
      return false;
    }
    if (!/^\d{7,20}$/.test(this.Register.Phone)) {
      this.toastrService.warning('El teléfono debe contener entre 7 y 20 números.');
      return false;
    }
    if (this.Register.Email.length > 32 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.Register.Email)) {
      this.toastrService.warning('Ingresa un correo válido de hasta 32 caracteres.');
      return false;
    }
    if (this.Register.Password.length < 8 || this.Register.Password.length > 72) {
      this.toastrService.warning('La contraseña debe contener entre 8 y 72 caracteres.');
      return false;
    }
    if (this.Register.Password !== this.Register.ConfirmPassword) {
      this.toastrService.warning('Las contraseñas no coinciden.');
      return false;
    }
    return true;
  }
}
