import { Component, ElementRef, EventEmitter, Input, OnChanges, OnDestroy, Output, SimpleChanges, ViewChild } from '@angular/core';
import * as L from 'leaflet';
import { ToastrService } from 'ngx-toastr';
import { AddressGeocodingResultDto } from '../../model/dto/AddressGeocodingResultDto';
import { LocationOptionDto } from '../../model/dto/LocationOptionDto';
import { ClientAddressEntity } from '../../model/entity/ClientAddressEntity';
import { ClientAddressService } from '../../service/client-address.service';

@Component({
  selector: 'app-address-modal',
  templateUrl: './address-modal.component.html',
  styleUrls: ['./address-modal.component.css']
})
export class AddressModalComponent implements OnChanges, OnDestroy {
  private readonly PeruCountryCod: string = 'PER';

  @ViewChild('mapContainer') private mapContainer?: ElementRef<HTMLDivElement>;
  @Input() public Visible: boolean = false;
  @Input() public InitialLatitude: number | null = null;
  @Input() public InitialLongitude: number | null = null;
  @Input() public DefaultNames: string = '';
  @Input() public DefaultPhone: string = '';
  @Input() public AddressToEdit: ClientAddressEntity | null = null;
  @Output() public Closed = new EventEmitter<void>();
  @Output() public Saved = new EventEmitter<ClientAddressEntity>();

  public Address = new ClientAddressEntity();
  public IsSaving: boolean = false;
  public IsLocating: boolean = false;
  public IsLoadingUbigeo: boolean = false;
  public IsSearchingAddress: boolean = false;
  public IsResolvingMapAddress: boolean = false;
  public AddressSearchText: string = '';
  public AddressSearchResultList: AddressGeocodingResultDto[] = [];
  public DepartmentCod: string = '';
  public ProvinceCod: string = '';
  public StateId: number | null = null;
  public CityId: number | null = null;
  public CountryList: LocationOptionDto[] = [];
  public DepartmentList: LocationOptionDto[] = [];
  public ProvinceList: LocationOptionDto[] = [];
  public DistrictList: LocationOptionDto[] = [];
  public StateList: LocationOptionDto[] = [];
  public CityList: LocationOptionDto[] = [];

  private map: L.Map | null = null;
  private marker: L.Marker | null = null;
  private accuracyCircle: L.Circle | null = null;
  private resizeObserver: ResizeObserver | null = null;
  private reverseGeocodingRequestNumber: number = 0;

  public constructor(
    private clientAddressService: ClientAddressService,
    private toastrService: ToastrService
  ) {
  }

  public ngOnChanges(changes: SimpleChanges): void {
    if (changes['Visible']?.currentValue === true) {
      this.resetAddress();
      void this.initializeLocation();
      setTimeout(() => this.initializeMap());
    }
    if (changes['Visible']?.currentValue === false) {
      this.destroyMap();
    }
  }

  public ngOnDestroy(): void {
    this.destroyMap();
  }

  public close(): void {
    if (!this.IsSaving) this.Closed.emit();
  }

  public useCurrentLocation(): void {
    if (!navigator.geolocation) {
      this.toastrService.warning('Tu navegador no permite detectar la ubicación. Selecciónala en el mapa.');
      return;
    }

    this.IsLocating = true;
    navigator.geolocation.getCurrentPosition(
      position => {
        this.IsLocating = false;
        void this.selectMapCoordinates(
          position.coords.latitude,
          position.coords.longitude,
          true
        );
        if (this.map) {
          if (this.accuracyCircle) this.map.removeLayer(this.accuracyCircle);
          this.accuracyCircle = L.circle(
            [position.coords.latitude, position.coords.longitude],
            {
              radius: position.coords.accuracy,
              color: '#2878bd',
              fillColor: '#2878bd',
              fillOpacity: 0.1,
              weight: 2
            }
          ).addTo(this.map);
        }
        this.toastrService.info(`Ubicación encontrada con una precisión aproximada de ${Math.round(position.coords.accuracy)} m.`);
      },
      () => {
        this.IsLocating = false;
        this.toastrService.info('No pudimos obtener tu ubicación. Puedes marcarla directamente en el mapa.');
      },
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
    );
  }

  public async save(): Promise<void> {
    if (!this.validate()) return;

    this.IsSaving = true;
    try {
      const response = await this.clientAddressService.save(this.Address);
      if (response.ErrorStatus || !response.Data?.ClientAddressID) {
        this.toastrService.error(response.Message || 'No se pudo guardar la dirección.');
        return;
      }
      const saved = Object.assign(new ClientAddressEntity(), response.Data);
      this.toastrService.success('Dirección guardada correctamente.');
      this.Saved.emit(saved);
    } finally {
      this.IsSaving = false;
    }
  }

  public isPeruCountry(): boolean {
    return this.Address.CountryCod === this.PeruCountryCod;
  }

  public countryChanged(): void {
    this.resetLocationSelections();
    this.Address.CountryName = this.optionName(this.CountryList, this.Address.CountryCod);
    this.Address.StateName = '';
    this.Address.CityName = '';
    this.Address.UbigeoCod = '';
    this.clearGeocodedAddress();
    this.AddressSearchResultList = [];

    if (this.isPeruCountry()) {
      void this.loadDepartments();
    } else if (this.Address.CountryCod) {
      void this.loadStates();
    }
  }

  public departmentChanged(): void {
    this.ProvinceCod = '';
    this.Address.UbigeoCod = '';
    this.ProvinceList = [];
    this.DistrictList = [];
    this.Address.StateName = this.optionName(this.DepartmentList, this.DepartmentCod);
    this.Address.CityName = '';
    if (this.DepartmentCod) void this.loadProvinces();
  }

  public async provinceChanged(): Promise<void> {
    this.Address.UbigeoCod = '';
    this.DistrictList = [];
    this.Address.CityName = '';
    if (this.ProvinceCod) {
      await this.loadDistricts();
      await this.centerPeruProvince();
    }
  }

  public districtChanged(): void {
    const selectedDistrict = this.DistrictList.find(
      district => district.Code === this.Address.UbigeoCod
    );
    if (!selectedDistrict) {
      this.Address.UbigeoCod = '';
      this.Address.CityName = '';
      return;
    }
    this.Address.CityName = selectedDistrict.Name;
  }

  public stateChanged(): void {
    this.CityId = null;
    this.Address.StateId = this.StateId;
    this.Address.CityId = null;
    this.Address.StateName = this.optionName(this.StateList, this.StateId);
    this.Address.CityName = '';
    this.Address.UbigeoCod = '';
    this.CityList = [];
    if (this.StateId) void this.loadCities();
  }

  public cityChanged(): void {
    this.Address.CityId = this.CityId;
    this.Address.CityName = this.optionName(this.CityList, this.CityId);
    this.Address.UbigeoCod = '';
    const selectedCity = this.CityList.find(city => city.Code === String(this.CityId));
    if (selectedCity) this.centerMapAtLocation(selectedCity);
  }

  public async searchAddress(): Promise<void> {
    if (!this.Address.CountryCod) {
      this.toastrService.warning('Selecciona primero el país de la dirección.');
      return;
    }
    if ((this.AddressSearchText || '').trim().length < 3) {
      this.toastrService.warning('Escribe al menos 3 caracteres para buscar una dirección.');
      return;
    }

    this.IsSearchingAddress = true;
    this.AddressSearchResultList = [];
    try {
      const response = await this.clientAddressService.searchAddress(
        this.buildAddressSearchQuery(),
        this.Address.CountryCod
      );
      if (response.ErrorStatus) {
        this.toastrService.error(response.Message || 'No se pudo buscar la dirección.');
        return;
      }
      this.AddressSearchResultList = Array.isArray(response.Data)
        ? response.Data.map((item: unknown) => Object.assign(new AddressGeocodingResultDto(), item))
        : [];
      if (this.AddressSearchResultList.length === 0) {
        this.toastrService.info('No encontramos coincidencias. Prueba con otra descripción o marca el punto en el mapa.');
      }
    } finally {
      this.IsSearchingAddress = false;
    }
  }

  public selectAddressSearchResult(result: AddressGeocodingResultDto): void {
    if (!this.validCoordinate(result.Latitude, -90, 90)
      || !this.validCoordinate(result.Longitude, -180, 180)) {
      this.toastrService.warning('La coincidencia seleccionada no tiene coordenadas válidas.');
      return;
    }
    this.reverseGeocodingRequestNumber++;
    this.IsResolvingMapAddress = false;
    this.Address.GeocodedAddress = result.DisplayName.substring(0, 512);
    if (!this.isPeruCountry() && result.PostalCode) {
      this.Address.UbigeoCod = result.PostalCode.substring(0, 12);
    }
    this.setCoordinates(Number(result.Latitude), Number(result.Longitude), true);
    this.AddressSearchResultList = [];
  }

  private resetAddress(): void {
    this.Address = this.AddressToEdit
      ? Object.assign(new ClientAddressEntity(), this.AddressToEdit)
      : new ClientAddressEntity();
    this.Address.Names = this.Address.Names || this.DefaultNames || '';
    this.Address.Phone = this.Address.Phone || this.DefaultPhone || '';
    this.AddressSearchText = '';
    this.AddressSearchResultList = [];
    if (!this.validCoordinate(this.Address.Latitude, -90, 90)) {
      this.Address.Latitude = this.validCoordinate(this.InitialLatitude, -90, 90)
        ? this.InitialLatitude
        : -6.7714;
    }
    if (!this.validCoordinate(this.Address.Longitude, -180, 180)) {
      this.Address.Longitude = this.validCoordinate(this.InitialLongitude, -180, 180)
        ? this.InitialLongitude
        : -79.8409;
    }
  }

  private async initializeLocation(): Promise<void> {
    this.CountryList = [];
    this.resetLocationSelections();
    this.IsLoadingUbigeo = true;

    try {
      const response = await this.clientAddressService.findCountries();
      if (response.ErrorStatus) {
        this.toastrService.error(response.Message || 'No se pudieron cargar los países.');
        return;
      }
      this.CountryList = this.mapLocationOptions(response.Data);
      if (!this.Address.CountryCod && /^\d{6}$/.test((this.Address.UbigeoCod || '').trim())) {
        this.Address.CountryCod = this.PeruCountryCod;
      }
      this.Address.CountryCod = this.Address.CountryCod || this.PeruCountryCod;
      if (!this.CountryList.some(country => country.Code === this.Address.CountryCod)) {
        this.Address.CountryCod = '';
        this.Address.CountryName = '';
        this.toastrService.warning('El país de la dirección ya no está disponible. Selecciona otro.');
        return;
      }
      this.Address.CountryName = this.optionName(this.CountryList, this.Address.CountryCod);

      if (this.isPeruCountry()) {
        await this.initializePeruLocation();
      } else {
        await this.initializeForeignLocation();
      }
    } finally {
      this.IsLoadingUbigeo = false;
    }
  }

  private async initializePeruLocation(): Promise<void> {
    await this.loadDepartments(false);
    const currentUbigeoCod = (this.Address.UbigeoCod || '').trim();
    if (!/^\d{6}$/.test(currentUbigeoCod)) {
      this.Address.UbigeoCod = '';
      return;
    }

    this.DepartmentCod = currentUbigeoCod.substring(0, 2);
    this.ProvinceCod = currentUbigeoCod.substring(0, 4);
    await this.loadProvinces(false);
    await this.loadDistricts(false);

    if (!this.DistrictList.some(district => district.Code === currentUbigeoCod)) {
      this.DepartmentCod = '';
      this.ProvinceCod = '';
      this.Address.UbigeoCod = '';
      this.ProvinceList = [];
      this.DistrictList = [];
      return;
    }
    this.Address.StateName = this.optionName(this.DepartmentList, this.DepartmentCod);
    this.Address.CityName = this.optionName(this.DistrictList, currentUbigeoCod);
  }

  private async initializeForeignLocation(): Promise<void> {
    await this.loadStates(false);
    const state = this.StateList.find(option => option.Name === this.Address.StateName);
    if (!state) return;

    this.StateId = Number(state.Code);
    this.Address.StateId = this.StateId;
    await this.loadCities(false);
    const city = this.CityList.find(option => option.Name === this.Address.CityName);
    if (!city) return;

    this.CityId = Number(city.Code);
    this.Address.CityId = this.CityId;
  }

  private async loadDepartments(updateLoading: boolean = true): Promise<void> {
    if (updateLoading) this.IsLoadingUbigeo = true;
    try {
      const response = await this.clientAddressService.findDepartments();
      if (response.ErrorStatus) {
        this.toastrService.error(response.Message || 'No se pudieron cargar los departamentos.');
        return;
      }
      this.DepartmentList = this.mapLocationOptions(response.Data);
    } finally {
      if (updateLoading) this.IsLoadingUbigeo = false;
    }
  }

  private async loadStates(updateLoading: boolean = true): Promise<void> {
    if (!this.Address.CountryCod) return;
    if (updateLoading) this.IsLoadingUbigeo = true;
    try {
      const response = await this.clientAddressService.findStates(this.Address.CountryCod);
      if (response.ErrorStatus) {
        this.toastrService.error(response.Message || 'No se pudieron cargar los estados.');
        return;
      }
      this.StateList = this.mapLocationOptions(response.Data);
    } finally {
      if (updateLoading) this.IsLoadingUbigeo = false;
    }
  }

  private async loadCities(updateLoading: boolean = true): Promise<void> {
    if (!this.StateId) return;
    if (updateLoading) this.IsLoadingUbigeo = true;
    try {
      const response = await this.clientAddressService.findCities(this.StateId);
      if (response.ErrorStatus) {
        this.toastrService.error(response.Message || 'No se pudieron cargar las ciudades.');
        return;
      }
      this.CityList = this.mapLocationOptions(response.Data);
    } finally {
      if (updateLoading) this.IsLoadingUbigeo = false;
    }
  }

  private async centerPeruProvince(): Promise<void> {
    const response = await this.clientAddressService.findPeruProvinceLocation(this.ProvinceCod);
    if (response.ErrorStatus || !response.Data) return;
    this.centerMapAtLocation(Object.assign(new LocationOptionDto(), response.Data));
  }

  private resetLocationSelections(): void {
    this.DepartmentCod = '';
    this.ProvinceCod = '';
    this.StateId = null;
    this.CityId = null;
    this.Address.StateId = null;
    this.Address.CityId = null;
    this.DepartmentList = [];
    this.ProvinceList = [];
    this.DistrictList = [];
    this.StateList = [];
    this.CityList = [];
  }

  private async loadProvinces(updateLoading: boolean = true): Promise<void> {
    if (!this.DepartmentCod) return;
    if (updateLoading) this.IsLoadingUbigeo = true;
    try {
      const response = await this.clientAddressService.findProvinces(this.DepartmentCod);
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
      const response = await this.clientAddressService.findDistricts(this.ProvinceCod);
      if (response.ErrorStatus) {
        this.toastrService.error(response.Message || 'No se pudieron cargar los distritos.');
        return;
      }
      this.DistrictList = this.mapLocationOptions(response.Data);
    } finally {
      if (updateLoading) this.IsLoadingUbigeo = false;
    }
  }

  private mapLocationOptions(data: unknown): LocationOptionDto[] {
    if (!Array.isArray(data)) return [];
    return data.map(item => Object.assign(new LocationOptionDto(), item));
  }

  private optionName(options: LocationOptionDto[], code: string | number | null): string {
    if (code === null || code === '') return '';
    return options.find(option => option.Code === String(code))?.Name || '';
  }

  private centerMapAtLocation(location: LocationOptionDto): void {
    if (!this.validCoordinate(location.Latitude, -90, 90)
      || !this.validCoordinate(location.Longitude, -180, 180)) return;
    this.clearGeocodedAddress();
    this.setCoordinates(Number(location.Latitude), Number(location.Longitude), true);
  }

  private buildAddressSearchQuery(): string {
    const parts = [this.AddressSearchText.trim()];
    if (this.isPeruCountry()) {
      this.addUniqueSearchPart(parts, this.optionName(this.ProvinceList, this.ProvinceCod));
      this.addUniqueSearchPart(parts, this.optionName(this.DepartmentList, this.DepartmentCod));
    } else {
      this.addUniqueSearchPart(parts, this.optionName(this.CityList, this.CityId));
      this.addUniqueSearchPart(parts, this.optionName(this.StateList, this.StateId));
    }
    this.addUniqueSearchPart(parts, this.optionName(this.CountryList, this.Address.CountryCod));
    return parts.join(', ');
  }

  private addUniqueSearchPart(parts: string[], value: string): void {
    if (!value) return;
    const normalizedValue = value.toLocaleUpperCase();
    if (!parts.some(part => part.toLocaleUpperCase().includes(normalizedValue))) {
      parts.push(value);
    }
  }

  private initializeMap(): void {
    const container = this.mapContainer?.nativeElement;
    if (!container || this.map) return;

    const latitude = Number(this.Address.Latitude);
    const longitude = Number(this.Address.Longitude);
    this.map = L.map(container).setView([latitude, longitude], 16);
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap',
      crossOrigin: true,
      keepBuffer: 4
    }).addTo(this.map);

    this.marker = L.marker([latitude, longitude], {
      draggable: true,
      icon: this.createMarkerIcon(),
      title: 'Dirección de entrega'
    }).addTo(this.map);
    this.marker.bindTooltip('ENTREGA', {
      permanent: true,
      direction: 'top',
      offset: [0, -30],
      className: 'address-map-label'
    });
    this.map.on('click', event => {
      void this.selectMapCoordinates(event.latlng.lat, event.latlng.lng);
    });
    this.marker.on('dragend', () => {
      const position = this.marker?.getLatLng();
      if (position) void this.selectMapCoordinates(position.lat, position.lng);
    });

    this.resizeObserver = new ResizeObserver(() => this.refreshMapSize());
    this.resizeObserver.observe(container);
    this.refreshMapSize();
    setTimeout(() => this.refreshMapSize(), 150);
    setTimeout(() => this.refreshMapSize(), 400);
  }

  private createMarkerIcon(): L.Icon {
    const markerSvg = `
      <svg xmlns="http://www.w3.org/2000/svg" width="40" height="52" viewBox="0 0 40 52">
        <path d="M20 1C9.5 1 1 9.5 1 20c0 14.2 19 31 19 31s19-16.8 19-31C39 9.5 30.5 1 20 1z"
          fill="#2878bd" stroke="#ffffff" stroke-width="2"/>
        <circle cx="20" cy="20" r="7" fill="#ffffff"/>
      </svg>`;
    return L.icon({
      iconUrl: `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(markerSvg)}`,
      iconSize: [40, 52],
      iconAnchor: [20, 51],
      tooltipAnchor: [0, -44]
    });
  }

  private refreshMapSize(): void {
    if (!this.map) return;
    requestAnimationFrame(() => {
      this.map?.invalidateSize({ animate: false, pan: false });
    });
  }

  private setCoordinates(latitude: number, longitude: number, centerMap: boolean = false): void {
    this.Address.Latitude = Number(latitude.toFixed(8));
    this.Address.Longitude = Number(longitude.toFixed(8));
    this.marker?.setLatLng([latitude, longitude]);
    if (centerMap) this.map?.setView([latitude, longitude], 17);
    if (this.accuracyCircle && this.map && !centerMap) {
      this.map.removeLayer(this.accuracyCircle);
      this.accuracyCircle = null;
    }
  }

  private async selectMapCoordinates(
    latitude: number,
    longitude: number,
    centerMap: boolean = false
  ): Promise<void> {
    this.setCoordinates(latitude, longitude, centerMap);
    this.clearGeocodedAddress();
    if (!this.Address.CountryCod) return;

    const requestNumber = this.reverseGeocodingRequestNumber;
    this.IsResolvingMapAddress = true;
    try {
      const response = await this.clientAddressService.findAddressByCoordinates(
        Number(this.Address.Latitude),
        Number(this.Address.Longitude),
        this.Address.CountryCod
      );
      if (requestNumber !== this.reverseGeocodingRequestNumber) return;
      if (response.ErrorStatus || !response.Data?.DisplayName) {
        this.toastrService.info(
          response.Message || 'Marcamos el punto, pero no encontramos una dirección aproximada.'
        );
        return;
      }
      const result = Object.assign(new AddressGeocodingResultDto(), response.Data);
      this.Address.GeocodedAddress = result.DisplayName.substring(0, 512);
      if (!this.isPeruCountry() && result.PostalCode) {
        this.Address.UbigeoCod = result.PostalCode.substring(0, 12);
      }
    } finally {
      if (requestNumber === this.reverseGeocodingRequestNumber) {
        this.IsResolvingMapAddress = false;
      }
    }
  }

  private clearGeocodedAddress(): void {
    this.reverseGeocodingRequestNumber++;
    this.Address.GeocodedAddress = '';
  }

  private validate(): boolean {
    if (!this.Address.CountryCod
      || !this.CountryList.some(country => country.Code === this.Address.CountryCod)) {
      this.toastrService.warning('Selecciona el país de la dirección.');
      return false;
    }
    this.Address.CountryName = this.optionName(this.CountryList, this.Address.CountryCod);

    if (this.isPeruCountry()) {
      if (!/^\d{6}$/.test((this.Address.UbigeoCod || '').trim())
        || !this.DistrictList.some(district => district.Code === this.Address.UbigeoCod)) {
        this.toastrService.warning('Selecciona el departamento, provincia y distrito de la dirección.');
        return false;
      }
      this.Address.StateName = this.optionName(this.DepartmentList, this.DepartmentCod);
      this.Address.CityName = this.optionName(this.DistrictList, this.Address.UbigeoCod);
      this.Address.StateId = null;
      this.Address.CityId = null;
    } else {
      const hasState = this.StateId !== null
        && this.StateList.some(state => state.Code === String(this.StateId));
      const hasCity = this.CityId !== null
        && this.CityList.some(city => city.Code === String(this.CityId));
      if (!hasState || !hasCity) {
        this.toastrService.warning('Selecciona el estado y la ciudad de la dirección.');
        return false;
      }
      const territorialCode = (this.Address.UbigeoCod || '').trim();
      if (!territorialCode || territorialCode.length > 12) {
        this.toastrService.warning('Ingresa un código postal o territorial de hasta 12 caracteres.');
        return false;
      }
      this.Address.StateId = this.StateId;
      this.Address.CityId = this.CityId;
      this.Address.StateName = this.optionName(this.StateList, this.StateId);
      this.Address.CityName = this.optionName(this.CityList, this.CityId);
      this.Address.UbigeoCod = territorialCode;
    }
    if (!this.Address.Address.trim()) {
      this.toastrService.warning('Escribe la dirección que corresponde al punto marcado.');
      return false;
    }
    if (!this.Address.Names.trim() || !this.Address.Phone.trim()) {
      this.toastrService.warning('Ingresa el nombre y teléfono de contacto para esta dirección.');
      return false;
    }
    if (!this.validCoordinate(this.Address.Latitude, -90, 90)
      || !this.validCoordinate(this.Address.Longitude, -180, 180)) {
      this.toastrService.warning('Selecciona una ubicación válida en el mapa.');
      return false;
    }
    return true;
  }

  private validCoordinate(value: number | null, minimum: number, maximum: number): boolean {
    return value !== null && Number.isFinite(Number(value))
      && Number(value) >= minimum && Number(value) <= maximum;
  }

  private destroyMap(): void {
    this.reverseGeocodingRequestNumber++;
    this.IsResolvingMapAddress = false;
    this.resizeObserver?.disconnect();
    this.resizeObserver = null;
    if (this.map) this.map.remove();
    this.map = null;
    this.marker = null;
    this.accuracyCircle = null;
  }
}
