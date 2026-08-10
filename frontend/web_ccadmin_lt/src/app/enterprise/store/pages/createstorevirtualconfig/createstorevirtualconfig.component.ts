import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { StoreVirtualConfigEntity } from 'src/app/enterprise/sale/model/entity/StoreVirtualConfigEntity';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { StoreEntity } from 'src/app/enterprise/shared/model/entity/StoreEntity';
import { StoreVirtualConfigRegisterDto } from '../../model/dto/StoreVirtualConfigRegisterDto';
import { StoreService } from '../../service/store.service';

interface VirtualStoreReachPreset {
  Code: string;
  Name: string;
  Description: string;
  AutomaticDeliveryRadiusKm: number;
  ScheduledDeliveryMaxRadiusKm: number;
}

@Component({
  selector: 'app-createstorevirtualconfig',
  templateUrl: './createstorevirtualconfig.component.html'
})
export class CreatestorevirtualconfigComponent implements OnInit {

  StoreCod: string = "";
  Store: StoreEntity = new StoreEntity();
  SelectedPreset: string = "LOCALITY";
  Config: StoreVirtualConfigEntity = new StoreVirtualConfigEntity();

  readonly ReachPresets: VirtualStoreReachPreset[] = [
    {
      Code: "LOCALITY",
      Name: "Vender solo en tu localidad",
      Description: "Delivery cercano y entregas programadas dentro de la localidad.",
      AutomaticDeliveryRadiusKm: 10,
      ScheduledDeliveryMaxRadiusKm: 30
    },
    {
      Code: "DEPARTMENT",
      Name: "Vender en todo tu departamento",
      Description: "Delivery cercano y envios programados dentro del departamento.",
      AutomaticDeliveryRadiusKm: 10,
      ScheduledDeliveryMaxRadiusKm: 750
    },
    {
      Code: "COUNTRY",
      Name: "Vender en todo el pais",
      Description: "Delivery cercano y envios programados o por agencia a nivel nacional.",
      AutomaticDeliveryRadiusKm: 10,
      ScheduledDeliveryMaxRadiusKm: 2500
    },
    {
      Code: "INTERNATIONAL",
      Name: "Vender en el extranjero",
      Description: "Delivery cercano y alcance internacional mediante operadores de envio.",
      AutomaticDeliveryRadiusKm: 10,
      ScheduledDeliveryMaxRadiusKm: 20000
    }
  ];

  constructor(
    private storeService: StoreService,
    private router: Router,
    private toastrService: ToastrService
  ) {
    const urlTree: any = this.router.parseUrl(this.router.url);
    this.StoreCod = urlTree.queryParams['StoreCod'] ?? "";
  }

  async ngOnInit(): Promise<void> {
    if (!this.StoreCod) {
      this.toastrService.error("No se indico la tienda que se configurara.");
      this.returnToList();
      return;
    }
    await this.loadData();
  }

  async loadData(): Promise<void> {
    const [storeResponse, configResponse]: ResponseWsDto[] = await Promise.all([
      this.storeService.FindById(this.StoreCod),
      this.storeService.FindVirtualConfig(this.StoreCod)
    ]);

    if (storeResponse.ErrorStatus || configResponse.ErrorStatus) return;

    this.Store = Object.assign(new StoreEntity(), storeResponse.Data ?? {});
    this.Config = Object.assign(new StoreVirtualConfigEntity(), configResponse.Data ?? {});
    this.Config.StoreCod = this.StoreCod;

    if (!configResponse.Data?.CreationUser) {
      this.SelectedPreset = "LOCALITY";
      this.applySelectedPreset();
    } else {
      this.SelectedPreset = "CUSTOM";
    }
  }

  applySelectedPreset(): void {
    const preset = this.ReachPresets.find(item => item.Code === this.SelectedPreset);
    if (!preset) return;

    this.Config.AllowsAutomaticDelivery = "S";
    this.Config.AutomaticDeliveryRadiusKm = preset.AutomaticDeliveryRadiusKm;
    this.Config.AllowsScheduledDelivery = "S";
    this.Config.ScheduledDeliveryMaxRadiusKm = preset.ScheduledDeliveryMaxRadiusKm;
    this.Config.AllowsStorePickup = "S";
    this.Config.PreparationTimeMinutes = 60;
  }

  markAsCustom(): void {
    this.SelectedPreset = "CUSTOM";
  }

  get deliveryOptionsEnabled(): boolean {
    return this.Store.IsVirtualStoreEnabled === "S" && this.hasCompleteVirtualLocation();
  }

  async save(): Promise<void> {
    if (!this.validate()) return;

    const register = new StoreVirtualConfigRegisterDto();
    register.Store = this.Store;
    register.Config = this.Config;

    const response: ResponseWsDto = await this.storeService.SaveVirtualConfig(register);
    if (!response.ErrorStatus) {
      this.toastrService.success("Configuracion de tienda virtual guardada correctamente.");
      this.returnToList();
    }
  }

  validate(): boolean {
    if (this.Store.IsVirtualStoreEnabled !== "S" && this.Store.IsVirtualStoreEnabled !== "N") {
      this.toastrService.error("Indique si la tienda participara en la tienda virtual.");
      return false;
    }
    if (this.Store.IsVirtualStoreEnabled === "N") return true;

    if (!this.hasCompleteVirtualLocation()) {
      this.toastrService.error("Complete direccion, ubigeo, latitud y longitud antes de configurar el delivery.");
      return false;
    }

    const latitude = Number(this.Store.Latitude);
    const longitude = Number(this.Store.Longitude);
    if (latitude < -90 || latitude > 90) {
      this.toastrService.error("La latitud debe encontrarse entre -90 y 90.");
      return false;
    }
    if (longitude < -180 || longitude > 180) {
      this.toastrService.error("La longitud debe encontrarse entre -180 y 180.");
      return false;
    }

    const automaticRadius = Number(this.Config.AutomaticDeliveryRadiusKm);
    const scheduledRadius = Number(this.Config.ScheduledDeliveryMaxRadiusKm);
    const preparationTime = Number(this.Config.PreparationTimeMinutes);

    if (this.Config.AllowsAutomaticDelivery === "S"
      && (!Number.isFinite(automaticRadius) || automaticRadius <= 0)) {
      this.toastrService.error("Ingrese un radio mayor que cero para el delivery automatico.");
      return false;
    }
    if (this.Config.AllowsScheduledDelivery === "S"
      && (!Number.isFinite(scheduledRadius) || scheduledRadius <= 0)) {
      this.toastrService.error("Ingrese un alcance mayor que cero para la entrega programada.");
      return false;
    }
    if (Number.isFinite(automaticRadius) && Number.isFinite(scheduledRadius)
      && scheduledRadius < automaticRadius) {
      this.toastrService.error("El alcance programado no puede ser menor al radio automatico.");
      return false;
    }
    if (!Number.isInteger(preparationTime) || preparationTime < 0) {
      this.toastrService.error("El tiempo de preparacion debe ser un numero entero no negativo.");
      return false;
    }
    return true;
  }

  private hasCompleteVirtualLocation(): boolean {
    const latitude = Number(this.Store.Latitude);
    const longitude = Number(this.Store.Longitude);

    return !!this.Store.Address?.trim()
      && !!this.Store.UbigeoCod?.trim()
      && this.Store.Latitude !== null
      && this.Store.Latitude !== undefined
      && Number.isFinite(latitude)
      && latitude >= -90
      && latitude <= 90
      && this.Store.Longitude !== null
      && this.Store.Longitude !== undefined
      && Number.isFinite(longitude)
      && longitude >= -180
      && longitude <= 180;
  }

  returnToList(): void {
    this.router.navigate(['/enterprise/store/pages/liststore']);
  }
}
