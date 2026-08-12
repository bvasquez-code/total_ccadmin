import { Component, OnInit } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { ClientProfileDto } from '../../model/dto/ClientProfileDto';
import { ClientProfileUpdateDto } from '../../model/dto/ClientProfileUpdateDto';
import { ClientAddressEntity } from '../../model/entity/ClientAddressEntity';
import { ClientAddressService } from '../../service/client-address.service';
import { ClientProfileService } from '../../service/client-profile.service';
import { ClientSessionService } from '../../service/client-session.service';

@Component({
  selector: 'app-client-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css']
})
export class ProfileComponent implements OnInit {
  public Profile = new ClientProfileDto();
  public AddressList: ClientAddressEntity[] = [];
  public EditingAddress: ClientAddressEntity | null = null;
  public IsAddressModalVisible: boolean = false;
  public IsLoadingProfile: boolean = false;
  public IsLoadingAddresses: boolean = false;
  public IsSavingProfile: boolean = false;

  public constructor(
    private clientProfileService: ClientProfileService,
    private clientAddressService: ClientAddressService,
    private clientSessionService: ClientSessionService,
    private toastrService: ToastrService
  ) {
  }

  public ngOnInit(): void {
    void this.loadProfile();
    void this.loadAddresses();
  }

  public async saveProfile(): Promise<void> {
    const request = new ClientProfileUpdateDto();
    request.Names = this.Profile.Names.trim();
    request.LastNames = this.Profile.LastNames.trim();
    request.Phone = this.Profile.Phone.trim();
    if (!this.validateProfile(request)) return;

    this.IsSavingProfile = true;
    try {
      const response = await this.clientProfileService.update(request);
      if (response.ErrorStatus || !response.Data) {
        this.toastrService.error(response.Message || 'No se pudo actualizar el perfil.');
        return;
      }
      this.Profile = Object.assign(new ClientProfileDto(), response.Data);
      this.clientSessionService.updateNames(`${this.Profile.Names} ${this.Profile.LastNames}`.trim());
      this.toastrService.success('Tus datos fueron actualizados correctamente.');
    } finally {
      this.IsSavingProfile = false;
    }
  }

  public openNewAddress(): void {
    this.EditingAddress = null;
    this.IsAddressModalVisible = true;
  }

  public editAddress(address: ClientAddressEntity): void {
    this.EditingAddress = Object.assign(new ClientAddressEntity(), address);
    this.IsAddressModalVisible = true;
  }

  public closeAddressModal(): void {
    this.IsAddressModalVisible = false;
    this.EditingAddress = null;
  }

  public async addressSaved(): Promise<void> {
    this.closeAddressModal();
    await this.loadAddresses();
  }

  public documentTypeName(): string {
    if (this.Profile.DocumentType === '01') return 'DNI';
    if (this.Profile.DocumentType === '04') return 'Carnet de extranjería';
    return this.Profile.DocumentType || 'Documento';
  }

  private async loadProfile(): Promise<void> {
    this.IsLoadingProfile = true;
    try {
      const response = await this.clientProfileService.find();
      if (response.ErrorStatus || !response.Data) {
        this.toastrService.error(response.Message || 'No se pudo consultar el perfil.');
        return;
      }
      this.Profile = Object.assign(new ClientProfileDto(), response.Data);
    } finally {
      this.IsLoadingProfile = false;
    }
  }

  private async loadAddresses(): Promise<void> {
    this.IsLoadingAddresses = true;
    try {
      const response = await this.clientAddressService.findAll();
      if (response.ErrorStatus) {
        this.toastrService.error(response.Message || 'No se pudieron consultar tus direcciones.');
        return;
      }
      this.AddressList = (response.Data || []).map(
        (item: ClientAddressEntity) => Object.assign(new ClientAddressEntity(), item)
      );
    } finally {
      this.IsLoadingAddresses = false;
    }
  }

  private validateProfile(request: ClientProfileUpdateDto): boolean {
    if (!request.Names || !request.LastNames) {
      this.toastrService.warning('Los nombres y apellidos son obligatorios.');
      return false;
    }
    if (request.Names.length > 128 || request.LastNames.length > 128) {
      this.toastrService.warning('Los nombres y apellidos admiten hasta 128 caracteres.');
      return false;
    }
    if (!/^\d{7,20}$/.test(request.Phone)) {
      this.toastrService.warning('El teléfono debe contener entre 7 y 20 números.');
      return false;
    }
    return true;
  }
}
