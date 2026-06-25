import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { ValidationHelper } from 'src/app/enterprise/shared/helper/ValidationHelper';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { StoreEntity } from 'src/app/enterprise/shared/model/entity/StoreEntity';
import { StoreService } from '../../service/store.service';

@Component({
  selector: 'app-createstore',
  templateUrl: './createstore.component.html'
})
export class CreatestoreComponent implements OnInit {

  StoreCod: string = "";
  Store: StoreEntity = new StoreEntity();
  txtStoreCodreadonly: boolean = false;

  constructor(
    private storeService: StoreService,
    private router: Router,
    private toastrService: ToastrService
  ) {
    this.GetParamUrl(this.router);
  }

  ngOnInit(): void {
    if (this.StoreCod !== "") this.FindById(this.StoreCod);
  }

  GetParamUrl(router: Router): void {
    let urlTree: any = router.parseUrl(this.router.url);
    this.StoreCod = (urlTree.queryParams['StoreCod']) ? urlTree.queryParams['StoreCod'] : "";
  }

  async FindById(StoreCod: string): Promise<void> {
    const rpt: ResponseWsDto = await this.storeService.FindById(StoreCod);

    if (!rpt.ErrorStatus) {
      this.Store = rpt.Data;
      this.txtStoreCodreadonly = true;
    }
  }

  async Save(): Promise<void> {
    if (!this.Store) this.Store = new StoreEntity();

    if (!this.validate(this.Store)) return;

    const rpt: ResponseWsDto = await this.storeService.Save(this.Store);

    if (!rpt.ErrorStatus) {
      this.toastrService.success("Operacion realizada con exito.");
      this.router.navigate(['/enterprise/store/pages/liststore']);
    }
  }

  validate(store: StoreEntity) {
    try {
      ValidationHelper.validLengthString(store.StoreCod, 4, "El codigo de tienda solo puede tener 4 caracteres");
      ValidationHelper.validateIsNotEmpty(store.StoreCod, "Debe ingresar un codigo para la tienda");

      ValidationHelper.validLengthString(store.Name, 32, "El nombre de la tienda solo puede tener 32 caracteres");
      ValidationHelper.validateIsNotEmpty(store.Name, "Debe ingresar un nombre para la tienda");

      ValidationHelper.validLengthString(store.Description, 128, "La descripcion de la tienda solo puede tener 128 caracteres");
      ValidationHelper.validLengthString(store.Address, 128, "La direccion de la tienda solo puede tener 128 caracteres");
      ValidationHelper.validLengthString(store.UbigeoCod, 12, "El ubigeo solo puede tener 12 caracteres");

      return true;
    } catch (e: any) {
      this.toastrService.error(e.message);
      return false;
    }
  }

  validateKeypress(event: KeyboardEvent, id: string) {
    try {
      if (id === "txtStoreCod" || id === "txtUbigeoCod") {
        ValidationHelper.isValidString(event.key.toString(), "Error", /[a-zA-Z0-9]/);
      }
    } catch (e: any) {
      event.preventDefault();
    }
  }

}
