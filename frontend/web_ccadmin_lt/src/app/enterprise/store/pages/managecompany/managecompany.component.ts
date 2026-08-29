import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { PersonIdentityLookupService } from 'src/app/enterprise/person/service/person-identity-lookup.service';
import { ValidationHelper } from 'src/app/enterprise/shared/helper/ValidationHelper';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { CompanyEntity } from 'src/app/enterprise/shared/model/entity/CompanyEntity';
import { LocationOptionDto } from '../../model/dto/LocationOptionDto';
import { CompanyService } from '../../service/company.service';
import { StoreService } from '../../service/store.service';

@Component({
  selector: 'app-managecompany',
  templateUrl: './managecompany.component.html'
})
export class ManagecompanyComponent implements OnInit {

  Company: CompanyEntity = new CompanyEntity();
  DepartmentCod: string = '';
  ProvinceCod: string = '';
  DepartmentList: LocationOptionDto[] = [];
  ProvinceList: LocationOptionDto[] = [];
  DistrictList: LocationOptionDto[] = [];
  IsExistingCompany: boolean = false;
  IsLoading: boolean = false;
  IsLoadingUbigeo: boolean = false;
  IsSearchingIdentity: boolean = false;
  IsSaving: boolean = false;

  public constructor(
    private companyService: CompanyService,
    private storeService: StoreService,
    private personIdentityLookupService: PersonIdentityLookupService,
    private router: Router,
    private toastrService: ToastrService
  ) {
  }

  public async ngOnInit(): Promise<void> {
    await this.loadData();
  }

  public async searchByRuc(): Promise<void> {
    if (this.IsSearchingIdentity) return;

    this.Company.TaxId = (this.Company.TaxId || '').trim();
    if (!/^\d{11}$/.test(this.Company.TaxId)) {
      this.toastrService.error('El RUC debe tener 11 dígitos.');
      return;
    }

    this.IsSearchingIdentity = true;
    try {
      const identityResult = await this.personIdentityLookupService.findByDocument(
        '06',
        this.Company.TaxId
      );
      const person = identityResult.person;
      if (!person?.BusinessName) {
        this.toastrService.info('No se encontraron datos para el RUC ingresado.');
        return;
      }

      this.Company.LegalName = person.BusinessName.trim();
      this.Company.TradeName = (person.CommercialName || '').trim();
      if (person.Address?.trim()) {
        this.Company.FiscalAddress = person.Address.trim();
        this.Company.Address = person.Address.trim().substring(0, 128);
      }

      this.toastrService.success(
        identityResult.source === 'SUNAT'
          ? 'Datos obtenidos correctamente desde el servicio de identidad.'
          : 'Datos encontrados en el sistema.'
      );
    } catch (error: any) {
      this.toastrService.error(error?.message || 'No fue posible consultar el RUC.');
    } finally {
      this.IsSearchingIdentity = false;
    }
  }

  public async departmentChanged(): Promise<void> {
    this.ProvinceCod = '';
    this.Company.Province = '';
    this.Company.District = '';
    this.Company.UbigeoCod = '';
    this.ProvinceList = [];
    this.DistrictList = [];
    this.Company.Department = this.optionName(this.DepartmentList, this.DepartmentCod);
    if (this.DepartmentCod) await this.loadProvinces();
  }

  public async provinceChanged(): Promise<void> {
    this.Company.District = '';
    this.Company.UbigeoCod = '';
    this.DistrictList = [];
    this.Company.Province = this.optionName(this.ProvinceList, this.ProvinceCod);
    if (this.ProvinceCod) await this.loadDistricts();
  }

  public districtChanged(): void {
    this.Company.District = this.optionName(this.DistrictList, this.Company.UbigeoCod);
  }

  public async save(): Promise<void> {
    if (this.IsSaving || !this.validate()) return;

    this.IsSaving = true;
    try {
      const response: ResponseWsDto = await this.companyService.save(this.Company);
      if (response.ErrorStatus) return;

      this.Company = Object.assign(new CompanyEntity(), response.Data ?? {});
      this.IsExistingCompany = true;
      this.toastrService.success('Compañía guardada correctamente.');
    } finally {
      this.IsSaving = false;
    }
  }

  public validateKeypress(event: KeyboardEvent, field: string): void {
    try {
      if (field === 'CompanyCod') {
        ValidationHelper.isValidString(event.key, 'Error', /^[a-zA-Z0-9]$/);
      }
      if (field === 'TaxId') {
        ValidationHelper.isValidString(event.key, 'Error', /^\d$/);
      }
    } catch {
      event.preventDefault();
    }
  }

  public returnToStores(): void {
    this.router.navigate(['/enterprise/store/pages/liststore']);
  }

  private async loadData(): Promise<void> {
    this.IsLoading = true;
    try {
      const [companyResponse, departmentResponse]: ResponseWsDto[] = await Promise.all([
        this.companyService.find(),
        this.storeService.FindDepartments()
      ]);

      if (companyResponse.ErrorStatus) {
        this.toastrService.error(companyResponse.Message || 'No se pudo cargar la compañía.');
      } else if (companyResponse.Data) {
        this.Company = Object.assign(new CompanyEntity(), companyResponse.Data);
        this.IsExistingCompany = true;
      }

      this.Company.CountryCode = 'PE';
      this.normalizeNullableFields();

      if (departmentResponse.ErrorStatus) {
        this.toastrService.error(
          departmentResponse.Message || 'No se pudieron cargar los departamentos.'
        );
        return;
      }
      this.DepartmentList = this.mapLocationOptions(departmentResponse.Data);
      await this.initializeUbigeoSelection();
    } finally {
      this.IsLoading = false;
    }
  }

  private async initializeUbigeoSelection(): Promise<void> {
    const ubigeoCod = (this.Company.UbigeoCod || '').trim();
    if (!/^\d{6}$/.test(ubigeoCod)) {
      this.Company.UbigeoCod = '';
      return;
    }

    this.DepartmentCod = ubigeoCod.substring(0, 2);
    this.ProvinceCod = ubigeoCod.substring(0, 4);
    await this.loadProvinces(false);
    await this.loadDistricts(false);

    if (!this.DistrictList.some(district => district.Code === ubigeoCod)) {
      this.DepartmentCod = '';
      this.ProvinceCod = '';
      this.Company.UbigeoCod = '';
      this.ProvinceList = [];
      this.DistrictList = [];
      this.toastrService.warning(
        'El ubigeo registrado ya no está disponible. Seleccione uno nuevo.'
      );
      return;
    }

    this.Company.Department = this.optionName(this.DepartmentList, this.DepartmentCod);
    this.Company.Province = this.optionName(this.ProvinceList, this.ProvinceCod);
    this.Company.District = this.optionName(this.DistrictList, ubigeoCod);
  }

  private async loadProvinces(updateLoading: boolean = true): Promise<void> {
    if (!this.DepartmentCod) return;
    if (updateLoading) this.IsLoadingUbigeo = true;
    try {
      const response: ResponseWsDto = await this.storeService.FindProvinces(this.DepartmentCod);
      if (response.ErrorStatus) {
        this.toastrService.error(response.Message || 'No se pudieron cargar las provincias.');
        return;
      }
      this.ProvinceList = this.mapLocationOptions(response.Data);
    } finally {
      if (updateLoading) this.IsLoadingUbigeo = false;
    }
  }

  private async loadDistricts(updateLoading: boolean = true): Promise<void> {
    if (!this.ProvinceCod) return;
    if (updateLoading) this.IsLoadingUbigeo = true;
    try {
      const response: ResponseWsDto = await this.storeService.FindDistricts(this.ProvinceCod);
      if (response.ErrorStatus) {
        this.toastrService.error(response.Message || 'No se pudieron cargar los distritos.');
        return;
      }
      this.DistrictList = this.mapLocationOptions(response.Data);
    } finally {
      if (updateLoading) this.IsLoadingUbigeo = false;
    }
  }

  private validate(): boolean {
    try {
      this.Company.CompanyCod = (this.Company.CompanyCod || '').trim().toUpperCase();
      this.Company.TaxId = (this.Company.TaxId || '').trim();
      this.Company.CountryCode = 'PE';

      if (!/^[A-Z0-9]{4}$/.test(this.Company.CompanyCod)) {
        throw new Error('El código de compañía debe tener 4 letras o números.');
      }
      if (!/^\d{11}$/.test(this.Company.TaxId)) {
        throw new Error('El RUC debe tener 11 dígitos.');
      }

      ValidationHelper.validateIsNotEmpty(
        this.Company.LegalName,
        'Debe ingresar la razón social.'
      );
      ValidationHelper.validLengthString(
        this.Company.LegalName,
        200,
        'La razón social solo puede tener 200 caracteres.'
      );
      ValidationHelper.validLengthString(
        this.Company.TradeName || '',
        200,
        'El nombre comercial solo puede tener 200 caracteres.'
      );
      ValidationHelper.validateIsNotEmpty(
        this.Company.FiscalAddress,
        'Debe ingresar el domicilio fiscal.'
      );
      ValidationHelper.validLengthString(
        this.Company.FiscalAddress,
        300,
        'El domicilio fiscal solo puede tener 300 caracteres.'
      );
      ValidationHelper.validLengthString(
        this.Company.Address || '',
        128,
        'La dirección comercial solo puede tener 128 caracteres.'
      );
      ValidationHelper.validateIsNotEmpty(this.DepartmentCod, 'Debe seleccionar un departamento.');
      ValidationHelper.validateIsNotEmpty(this.ProvinceCod, 'Debe seleccionar una provincia.');
      if (!/^\d{6}$/.test(this.Company.UbigeoCod)) {
        throw new Error('Debe seleccionar un distrito válido.');
      }
      ValidationHelper.validLengthString(
        this.Company.Phone || '',
        30,
        'El teléfono solo puede tener 30 caracteres.'
      );
      ValidationHelper.validLengthString(
        this.Company.Email || '',
        150,
        'El correo solo puede tener 150 caracteres.'
      );
      if (this.Company.Email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.Company.Email)) {
        throw new Error('El correo ingresado no es válido.');
      }
      ValidationHelper.validLengthString(
        this.Company.Website || '',
        150,
        'El sitio web solo puede tener 150 caracteres.'
      );
      ValidationHelper.validLengthString(
        this.Company.LogoPath || '',
        500,
        'La ruta del logo solo puede tener 500 caracteres.'
      );
      return true;
    } catch (error: any) {
      this.toastrService.error(error.message);
      return false;
    }
  }

  private normalizeNullableFields(): void {
    this.Company.CompanyCod = this.Company.CompanyCod || '';
    this.Company.TaxId = this.Company.TaxId || '';
    this.Company.LegalName = this.Company.LegalName || '';
    this.Company.TradeName = this.Company.TradeName || '';
    this.Company.FiscalAddress = this.Company.FiscalAddress || '';
    this.Company.Address = this.Company.Address || '';
    this.Company.UbigeoCod = this.Company.UbigeoCod || '';
    this.Company.Department = this.Company.Department || '';
    this.Company.Province = this.Company.Province || '';
    this.Company.District = this.Company.District || '';
    this.Company.Phone = this.Company.Phone || '';
    this.Company.Email = this.Company.Email || '';
    this.Company.Website = this.Company.Website || '';
    this.Company.LogoPath = this.Company.LogoPath || '';
  }

  private mapLocationOptions(data: unknown): LocationOptionDto[] {
    if (!Array.isArray(data)) return [];
    return data.map(item => Object.assign(new LocationOptionDto(), item));
  }

  private optionName(options: LocationOptionDto[], code: string): string {
    return options.find(option => option.Code === code)?.Name || '';
  }
}
