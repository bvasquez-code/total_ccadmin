import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { ValidationHelper } from '../../../shared/helper/ValidationHelper';
import { SunatInitializationForm, SunatInitializationMode } from '../../model/SunatInitializationModels';
import { SunatInitializationService } from '../../service/sunat-initialization.service';

@Component({
  selector: 'app-configuresunat',
  templateUrl: './configuresunat.component.html'
})
export class ConfigureSunatComponent implements OnInit {
  @Output() ConfigurationCompleted = new EventEmitter<void>();
  Mode: SunatInitializationMode = 'NONE';
  Form?: SunatInitializationForm;
  SolUser = '';
  SolPassword = '';
  CertificatePassword = '';
  GuideEnabled = false;
  GuideClientId = '';
  GuideClientSecret = '';
  Certificate?: File;
  IsLoading = false;
  IsSaving = false;
  LoadError = '';

  constructor(private sunatInitializationService: SunatInitializationService,
              private toastrService: ToastrService) {}

  ngOnInit(): void { void this.load(); }

  async load(): Promise<void> {
    this.IsLoading = true;
    this.Form = undefined;
    this.LoadError = '';
    try {
      const response = await this.sunatInitializationService.findDataForm();
      if (!response || response.ErrorStatus || !response.Data) {
        throw new Error(response?.Message || 'No se pudo cargar la configuración de SUNAT');
      }
      const form: SunatInitializationForm = response.Data;
      this.Form = form;
      this.Mode = form.Mode;
      this.SolUser = form.SolUser || '';
      this.GuideEnabled = form.GuideEnabled;
      this.GuideClientId = form.GuideClientId || '';
      this.clearSecrets();
    } catch (error: any) {
      this.LoadError = error.message || 'No se pudo cargar la configuración de SUNAT';
    } finally { this.IsLoading = false; }
  }

  selectCertificate(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.Certificate = undefined;
    const file = input.files?.[0];
    if (!file) return;
    if (!file.name.toLowerCase().endsWith('.pfx') || file.size > 2 * 1024 * 1024 || file.size === 0) {
      input.value = '';
      this.toastrService.error('Seleccione un certificado .pfx de hasta 2 MB');
      return;
    }
    this.Certificate = file;
  }

  async confirm(): Promise<void> {
    if (!this.Form || this.IsSaving || this.IsLoading) return;
    this.IsSaving = true;
    try {
      ValidationHelper.validateInList(this.Mode, ['NONE', 'BETA', 'PRODUCCION'], 'Seleccione cómo desea usar SUNAT');
      const production = this.Mode === 'PRODUCCION';
      if (production) {
        ValidationHelper.validateIsNotEmpty(this.SolUser, 'Ingrese el usuario SOL');
        if (!this.Form.HasSolPassword) ValidationHelper.validateIsNotEmpty(this.SolPassword, 'Ingrese la contraseña SOL');
        if (!this.Form.HasCertificate && !this.Certificate) throw new Error('Seleccione el certificado .pfx de producción');
        if (!this.Form.HasCertificatePassword || this.Certificate) {
          ValidationHelper.validateIsNotEmpty(this.CertificatePassword, 'Ingrese la contraseña del certificado');
        }
        if (this.GuideEnabled) {
          ValidationHelper.validateIsNotEmpty(this.GuideClientId, 'Ingrese el Client ID para las guías');
          if (!this.Form.HasGuideClientSecret || this.GuideClientId !== this.Form.GuideClientId) {
            ValidationHelper.validateIsNotEmpty(this.GuideClientSecret, 'Ingrese el Client Secret para las guías');
          }
        }
      }
      const configuration = {
        Mode: this.Mode,
        ...(production ? {
          SolUser: this.SolUser.trim(), SolPassword: this.SolPassword,
          CertificatePassword: this.CertificatePassword, GuideEnabled: this.GuideEnabled,
          GuideClientId: this.GuideEnabled ? this.GuideClientId.trim() : '',
          GuideClientSecret: this.GuideEnabled ? this.GuideClientSecret : ''
        } : {})
      };
      const response = await this.sunatInitializationService.configure(configuration, production ? this.Certificate : undefined);
      if (!response || response.ErrorStatus) throw new Error(response?.Message || 'No se pudo guardar la configuración de SUNAT');
      this.clearSecrets();
      this.toastrService.success(this.Mode === 'NONE' ? 'El envío a SUNAT quedó deshabilitado' : 'Configuración de SUNAT guardada');
      this.ConfigurationCompleted.emit();
    } catch (error: any) {
      this.toastrService.error(error.message || 'No se pudo guardar la configuración de SUNAT');
    } finally { this.IsSaving = false; }
  }

  private clearSecrets(): void {
    this.SolPassword = '';
    this.CertificatePassword = '';
    this.GuideClientSecret = '';
    this.Certificate = undefined;
  }
}
