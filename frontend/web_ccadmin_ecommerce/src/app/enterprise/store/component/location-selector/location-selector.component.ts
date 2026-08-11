import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { ResponseWsDto } from '../../../shared/model/dto/ResponseWsDto';
import { StoreContextDto } from '../../model/dto/StoreContextDto';
import { StoreLocationRequestDto } from '../../model/dto/StoreLocationRequestDto';
import { VirtualStoreService } from '../../service/virtual-store.service';

@Component({
  selector: 'app-location-selector',
  templateUrl: './location-selector.component.html',
  styleUrls: ['./location-selector.component.css']
})
export class LocationSelectorComponent {
  @Input() public Visible: boolean = false;
  @Output() public Closed = new EventEmitter<void>();
  @Output() public StoreChanged = new EventEmitter<StoreContextDto>();

  public Mode: 'automatic' | 'manual' = 'automatic';
  public ManualLocation = new StoreLocationRequestDto();
  public IsLocating: boolean = false;

  public constructor(
    private virtualStoreService: VirtualStoreService,
    private toastrService: ToastrService
  ) {
    this.ManualLocation.IsManual = 'S';
  }

  public close(): void {
    if (!this.IsLocating) this.Closed.emit();
  }

  public async detectByBrowser(): Promise<void> {
    if (!navigator.geolocation) {
      this.toastrService.warning('Tu navegador no permite detectar la ubicación. Puedes ingresarla manualmente.');
      this.Mode = 'manual';
      return;
    }

    this.IsLocating = true;
    navigator.geolocation.getCurrentPosition(
      position => {
        const request = new StoreLocationRequestDto();
        request.Latitude = position.coords.latitude;
        request.Longitude = position.coords.longitude;
        request.IsManual = 'N';
        void this.resolve(request);
      },
      () => {
        this.IsLocating = false;
        this.Mode = 'manual';
        this.toastrService.info('No pudimos obtener tu ubicación. Puedes indicarla manualmente.');
      },
      { enableHighAccuracy: false, timeout: 10000, maximumAge: 300000 }
    );
  }

  public async useManualLocation(): Promise<void> {
    const latitude = Number(this.ManualLocation.Latitude);
    const longitude = Number(this.ManualLocation.Longitude);

    if (!this.ManualLocation.Address.trim()) {
      this.toastrService.warning('Describe la dirección donde deseas recibir o recoger tu compra.');
      return;
    }
    if (!Number.isFinite(latitude) || latitude < -90 || latitude > 90
      || !Number.isFinite(longitude) || longitude < -180 || longitude > 180) {
      this.toastrService.warning('Ingresa coordenadas válidas para encontrar la tienda más cercana.');
      return;
    }

    this.ManualLocation.Latitude = latitude;
    this.ManualLocation.Longitude = longitude;
    await this.resolve(this.ManualLocation);
  }

  private async resolve(request: StoreLocationRequestDto): Promise<void> {
    this.IsLocating = true;
    const response: ResponseWsDto = await this.virtualStoreService.resolveLocation(request);
    this.IsLocating = false;

    if (response.ErrorStatus || !response.Data?.Store?.StoreCod) {
      this.toastrService.error(response.Message || 'No encontramos una tienda disponible para esa ubicación.');
      return;
    }

    const context = Object.assign(new StoreContextDto(), response.Data);
    this.StoreChanged.emit(context);
    this.Closed.emit();
  }
}
