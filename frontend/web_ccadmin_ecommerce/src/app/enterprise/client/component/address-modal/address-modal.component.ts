import { Component, ElementRef, EventEmitter, Input, OnChanges, OnDestroy, Output, SimpleChanges, ViewChild } from '@angular/core';
import * as L from 'leaflet';
import { ToastrService } from 'ngx-toastr';
import { ClientAddressEntity } from '../../model/entity/ClientAddressEntity';
import { ClientAddressService } from '../../service/client-address.service';

@Component({
  selector: 'app-address-modal',
  templateUrl: './address-modal.component.html',
  styleUrls: ['./address-modal.component.css']
})
export class AddressModalComponent implements OnChanges, OnDestroy {
  @ViewChild('mapContainer') private mapContainer?: ElementRef<HTMLDivElement>;
  @Input() public Visible: boolean = false;
  @Input() public InitialLatitude: number | null = null;
  @Input() public InitialLongitude: number | null = null;
  @Input() public DefaultNames: string = '';
  @Input() public DefaultPhone: string = '';
  @Output() public Closed = new EventEmitter<void>();
  @Output() public Saved = new EventEmitter<ClientAddressEntity>();

  public Address = new ClientAddressEntity();
  public IsSaving: boolean = false;
  public IsLocating: boolean = false;

  private map: L.Map | null = null;
  private marker: L.Marker | null = null;
  private accuracyCircle: L.Circle | null = null;
  private resizeObserver: ResizeObserver | null = null;

  public constructor(
    private clientAddressService: ClientAddressService,
    private toastrService: ToastrService
  ) {
  }

  public ngOnChanges(changes: SimpleChanges): void {
    if (changes['Visible']?.currentValue === true) {
      this.resetAddress();
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
        this.setCoordinates(position.coords.latitude, position.coords.longitude, true);
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

  private resetAddress(): void {
    this.Address = new ClientAddressEntity();
    this.Address.Names = this.DefaultNames || '';
    this.Address.Phone = this.DefaultPhone || '';
    this.Address.Latitude = this.validCoordinate(this.InitialLatitude, -90, 90)
      ? this.InitialLatitude
      : -6.7714;
    this.Address.Longitude = this.validCoordinate(this.InitialLongitude, -180, 180)
      ? this.InitialLongitude
      : -79.8409;
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
    this.map.on('click', event => this.setCoordinates(event.latlng.lat, event.latlng.lng));
    this.marker.on('dragend', () => {
      const position = this.marker?.getLatLng();
      if (position) this.setCoordinates(position.lat, position.lng);
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

  private validate(): boolean {
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
    this.resizeObserver?.disconnect();
    this.resizeObserver = null;
    if (this.map) this.map.remove();
    this.map = null;
    this.marker = null;
    this.accuracyCircle = null;
  }
}
