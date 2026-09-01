import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { ValidationHelper } from 'src/app/enterprise/shared/helper/ValidationHelper';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { CompanyEntity } from 'src/app/enterprise/shared/model/entity/CompanyEntity';
import { StoreEntity } from 'src/app/enterprise/shared/model/entity/StoreEntity';
import { LocationOptionDto } from '../../model/dto/LocationOptionDto';
import { StoreService } from '../../service/store.service';

@Component({
  selector: 'app-createstore',
  templateUrl: './createstore.component.html'
})
export class CreatestoreComponent implements OnInit {

  @Input() InitializationMode: boolean = false;
  @Input() InitialStoreCod: string = '';
  @Output() ConfigurationCompleted: EventEmitter<StoreEntity> =
    new EventEmitter<StoreEntity>();

  private readonly PeruCountryCod: string = 'PER';

  StoreCod: string = "";
  Store: StoreEntity = new StoreEntity();
  txtStoreCodreadonly: boolean = false;
  IsLoadingOptions: boolean = false;
  IsLoadingUbigeo: boolean = false;
  DepartmentCod: string = '';
  ProvinceCod: string = '';
  CompanyList: CompanyEntity[] = [];
  CountryList: LocationOptionDto[] = [];
  DepartmentList: LocationOptionDto[] = [];
  ProvinceList: LocationOptionDto[] = [];
  DistrictList: LocationOptionDto[] = [];

  constructor(
    private storeService: StoreService,
    private router: Router,
    private toastrService: ToastrService
  ) {
    this.GetParamUrl(this.router);
  }

  async ngOnInit(): Promise<void> {
    await this.LoadFormOptions();
    if (this.InitialStoreCod) {
      this.StoreCod = this.InitialStoreCod;
    }
    if (this.StoreCod !== "") {
      await this.FindById(this.StoreCod);
      return;
    }

    this.Store.CountryCod = this.CountryList.some(
      country => country.Code === this.PeruCountryCod
    ) ? this.PeruCountryCod : '';
    if (this.isPeruCountry()) await this.LoadDepartments();
  }

  GetParamUrl(router: Router): void {
    let urlTree: any = router.parseUrl(this.router.url);
    this.StoreCod = (urlTree.queryParams['StoreCod']) ? urlTree.queryParams['StoreCod'] : "";
  }

  async FindById(StoreCod: string): Promise<void> {
    const rpt: ResponseWsDto = await this.storeService.FindById(StoreCod);

    if (!rpt.ErrorStatus) {
      this.Store = Object.assign(new StoreEntity(), rpt.Data ?? {});
      this.Store.CountryCod = this.Store.CountryCod || this.PeruCountryCod;
      this.Store.CompanyCod = this.Store.CompanyCod || '';
      this.Store.UbigeoCod = this.Store.UbigeoCod || '';
      this.txtStoreCodreadonly = true;
      await this.InitializeUbigeoSelection();
    }
  }

  isPeruCountry(): boolean {
    return this.Store.CountryCod === this.PeruCountryCod;
  }

  async CountryChanged(): Promise<void> {
    this.ResetUbigeoSelection();
    this.Store.UbigeoCod = '';
    if (this.isPeruCountry()) await this.LoadDepartments();
  }

  async DepartmentChanged(): Promise<void> {
    this.ProvinceCod = '';
    this.Store.UbigeoCod = '';
    this.ProvinceList = [];
    this.DistrictList = [];
    if (this.DepartmentCod) await this.LoadProvinces();
  }

  async ProvinceChanged(): Promise<void> {
    this.Store.UbigeoCod = '';
    this.DistrictList = [];
    if (this.ProvinceCod) await this.LoadDistricts();
  }

  async Save(): Promise<void> {
    if (!this.Store) this.Store = new StoreEntity();

    if (!this.validate(this.Store)) return;

    const rpt: ResponseWsDto = await this.storeService.Save(this.Store);

    if (!rpt.ErrorStatus) {
      this.toastrService.success("Operacion realizada con exito.");
      if (this.InitializationMode) {
        this.ConfigurationCompleted.emit(
          Object.assign(new StoreEntity(), rpt.Data ?? this.Store)
        );
        return;
      }
      this.router.navigate(['/enterprise/store/pages/liststore']);
    }
  }

  validate(store: StoreEntity) {
    try {
      ValidationHelper.validLengthString(store.StoreCod, 4, "El codigo de tienda solo puede tener 4 caracteres");
      ValidationHelper.validateIsNotEmpty(store.StoreCod, "Debe ingresar un codigo para la tienda");

      ValidationHelper.validLengthString(store.Name, 32, "El nombre de la tienda solo puede tener 32 caracteres");
      ValidationHelper.validateIsNotEmpty(store.Name, "Debe ingresar un nombre para la tienda");
      if (this.InitializationMode
          && store.Name.trim().toUpperCase() === 'STORE_DEFAULT') {
        throw new Error("Debe reemplazar el nombre predeterminado de la tienda");
      }

      ValidationHelper.validLengthString(store.Description || '', 128, "La descripcion de la tienda solo puede tener 128 caracteres");
      ValidationHelper.validLengthString(store.Address || '', 128, "La direccion de la tienda solo puede tener 128 caracteres");
      ValidationHelper.validateIsNotEmpty(store.CompanyCod, "Debe seleccionar una compania");
      if (!this.CompanyList.some(company => company.CompanyCod === store.CompanyCod)) {
        throw new Error("La compania seleccionada no esta disponible");
      }

      ValidationHelper.validateIsNotEmpty(store.CountryCod, "Debe seleccionar un pais");
      if (!this.CountryList.some(country => country.Code === store.CountryCod)) {
        throw new Error("El pais seleccionado no esta disponible");
      }

      ValidationHelper.validLengthString(store.UbigeoCod, 12, "El ubigeo solo puede tener 12 caracteres");
      ValidationHelper.validateIsNotEmpty(
        store.UbigeoCod,
        this.isPeruCountry()
          ? "Debe seleccionar el distrito de la tienda"
          : "Debe ingresar el codigo postal o territorial de la tienda"
      );
      if (this.isPeruCountry()) {
        ValidationHelper.validateIsNotEmpty(this.DepartmentCod, "Debe seleccionar un departamento");
        ValidationHelper.validateIsNotEmpty(this.ProvinceCod, "Debe seleccionar una provincia");
        if (!/^\d{6}$/.test(store.UbigeoCod)) {
          throw new Error("El ubigeo de Peru debe tener 6 digitos");
        }
      }

      return true;
    } catch (e: any) {
      this.toastrService.error(e.message);
      return false;
    }
  }

  validateKeypress(event: KeyboardEvent, id: string) {
    try {
      if (id === "txtStoreCod") {
        ValidationHelper.isValidString(event.key.toString(), "Error", /[a-zA-Z0-9]/);
      }
      if (id === "txtUbigeoCod") {
        ValidationHelper.isValidString(event.key.toString(), "Error", /[a-zA-Z0-9 -]/);
      }
    } catch (e: any) {
      event.preventDefault();
    }
  }

  private async LoadFormOptions(): Promise<void> {
    this.IsLoadingOptions = true;
    try {
      const [companyResponse, countryResponse]: ResponseWsDto[] = await Promise.all([
        this.storeService.FindCompanies(),
        this.storeService.FindCountries()
      ]);

      if (companyResponse.ErrorStatus) {
        this.toastrService.error(companyResponse.Message || "No se pudieron cargar las companias");
      } else {
        this.CompanyList = Array.isArray(companyResponse.Data)
          ? companyResponse.Data.map((item: unknown) => Object.assign(new CompanyEntity(), item))
          : [];
      }

      if (countryResponse.ErrorStatus) {
        this.toastrService.error(countryResponse.Message || "No se pudieron cargar los paises");
      } else {
        this.CountryList = this.MapLocationOptions(countryResponse.Data);
      }
    } finally {
      this.IsLoadingOptions = false;
    }
  }

  private async InitializeUbigeoSelection(): Promise<void> {
    this.ResetUbigeoSelection();
    if (!this.CountryList.some(country => country.Code === this.Store.CountryCod)) {
      this.toastrService.warning("El pais de la tienda ya no esta disponible. Seleccione otro.");
      this.Store.CountryCod = '';
      return;
    }
    if (!this.isPeruCountry()) return;

    await this.LoadDepartments(false);
    const ubigeoCod = (this.Store.UbigeoCod || '').trim();
    if (!/^\d{6}$/.test(ubigeoCod)) {
      this.Store.UbigeoCod = '';
      return;
    }

    this.DepartmentCod = ubigeoCod.substring(0, 2);
    this.ProvinceCod = ubigeoCod.substring(0, 4);
    await this.LoadProvinces(false);
    await this.LoadDistricts(false);

    if (!this.DistrictList.some(district => district.Code === ubigeoCod)) {
      this.ResetUbigeoSelection();
      this.Store.UbigeoCod = '';
      this.toastrService.warning("El ubigeo registrado ya no esta disponible. Seleccione otro.");
    }
  }

  private ResetUbigeoSelection(): void {
    this.DepartmentCod = '';
    this.ProvinceCod = '';
    this.DepartmentList = [];
    this.ProvinceList = [];
    this.DistrictList = [];
  }

  private async LoadDepartments(updateLoading: boolean = true): Promise<void> {
    if (updateLoading) this.IsLoadingUbigeo = true;
    try {
      const response: ResponseWsDto = await this.storeService.FindDepartments();
      if (response.ErrorStatus) {
        this.toastrService.error(response.Message || "No se pudieron cargar los departamentos");
        return;
      }
      this.DepartmentList = this.MapLocationOptions(response.Data);
    } finally {
      if (updateLoading) this.IsLoadingUbigeo = false;
    }
  }

  private async LoadProvinces(updateLoading: boolean = true): Promise<void> {
    if (!this.DepartmentCod) return;
    if (updateLoading) this.IsLoadingUbigeo = true;
    try {
      const response: ResponseWsDto = await this.storeService.FindProvinces(this.DepartmentCod);
      if (response.ErrorStatus) {
        this.toastrService.error(response.Message || "No se pudieron cargar las provincias");
        return;
      }
      this.ProvinceList = this.MapLocationOptions(response.Data);
    } finally {
      if (updateLoading) this.IsLoadingUbigeo = false;
    }
  }

  private async LoadDistricts(updateLoading: boolean = true): Promise<void> {
    if (!this.ProvinceCod) return;
    if (updateLoading) this.IsLoadingUbigeo = true;
    try {
      const response: ResponseWsDto = await this.storeService.FindDistricts(this.ProvinceCod);
      if (response.ErrorStatus) {
        this.toastrService.error(response.Message || "No se pudieron cargar los distritos");
        return;
      }
      this.DistrictList = this.MapLocationOptions(response.Data);
    } finally {
      if (updateLoading) this.IsLoadingUbigeo = false;
    }
  }

  private MapLocationOptions(data: unknown): LocationOptionDto[] {
    if (!Array.isArray(data)) return [];
    return data.map(item => Object.assign(new LocationOptionDto(), item));
  }

}
