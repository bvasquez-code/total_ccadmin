import { AfterContentInit, AfterViewChecked, AfterViewInit, Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { AppMenuService } from '../../service/appmenu.service';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { AppMenuEntity } from '../../model/entity/AppMenuEntity';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { ModalService } from 'src/app/enterprise/shared/service/ModalService';
import { ValidationHelper } from 'src/app/enterprise/shared/helper/ValidationHelper';

@Component({
  selector: 'app-createmenu',
  templateUrl: './createmenu.component.html'
})
export class CreatemenuComponent {

  @ViewChild('txtMenuCod') txtMenuCod!: ElementRef<HTMLInputElement>;
  @ViewChild('txtName') txtName!: ElementRef<HTMLInputElement>;
  @ViewChild('txtDescription') txtDescription!: ElementRef<HTMLInputElement>;
  @ViewChild('cboIsMenuDad') cboIsMenuDad!: ElementRef<HTMLSelectElement>;
  @ViewChild('cboMenuDadCod') cboMenuDadCod!: ElementRef<HTMLSelectElement>;

  AppMenuDadList: AppMenuEntity[] = [];
  MenuCod: string = "";
  AppMenu: AppMenuEntity = new AppMenuEntity();

  txtMenuCodreadonly: boolean = false;
  cboMenuDadCodvisibility: boolean = true;

  constructor(
    private appMenuService: AppMenuService,
    private router: Router,
    private toastrService: ToastrService
  ) {

    let urlTree: any = this.router.parseUrl(this.router.url);
    this.MenuCod = urlTree.queryParams['MenuCod'];
    this.findDataForm(this.MenuCod);

  }


  async findDataForm(MenuCod: string) {
    const rpt: ResponseWsDto = await this.appMenuService.findDataForm(MenuCod);

    if (!rpt.Status) {
      this.AppMenuDadList = rpt.DataAdditional.find(e => e.Name === "AppMenuDadList")?.Data;
      this.AppMenu = rpt.DataAdditional.find(e => e.Name === "AppMenu")?.Data;

      setTimeout(() => { this.loadingForm(this.AppMenu); }, 100);

    }
  }

  loadingForm(AppMenu: AppMenuEntity) {
    console.log("loadingForm ....");
    this.txtMenuCod.nativeElement.value = AppMenu.MenuCod;
    this.txtName.nativeElement.value = AppMenu.Name;
    this.txtDescription.nativeElement.value = AppMenu.Description;
    this.cboIsMenuDad.nativeElement.value = AppMenu.IsMenuDad;
    this.cboMenuDadCod.nativeElement.value = AppMenu.MenuDadCod;

    if (AppMenu.MenuCod) this.txtMenuCodreadonly = true;
    if (this.cboIsMenuDad.nativeElement.value === "S") this.cboMenuDadCodvisibility = false;
  }

  async save() {
    if (!this.AppMenu) this.AppMenu = new AppMenuEntity();

    this.AppMenu.MenuCod = this.txtMenuCod.nativeElement.value;
    this.AppMenu.Name = this.txtName.nativeElement.value;
    this.AppMenu.Description = this.txtDescription.nativeElement.value;
    this.AppMenu.IsMenuDad = this.cboIsMenuDad.nativeElement.value;
    this.AppMenu.MenuDadCod = (this.AppMenu.IsMenuDad === "S") ? "" : this.cboMenuDadCod.nativeElement.value;

    if (!this.validate(this.AppMenu)) return;

    const rpt: ResponseWsDto = await this.appMenuService.save(this.AppMenu);

    if (!rpt.ErrorStatus) {
      this.router.navigate(['/enterprise/menu/pages/listmenu']);
    }
  }

  IsMenuDad() {
    this.cboMenuDadCodvisibility = (this.cboIsMenuDad.nativeElement.value === "S") ? false : true;
  }

  validate(appMenu: AppMenuEntity): boolean {
    try {
      ValidationHelper.validLengthString(appMenu.MenuCod, 8, "El código del menú solo puede tener 8 caracteres");
      ValidationHelper.validateIsNotEmpty(appMenu.MenuCod, "Debe ingresar un código para el menú");

      ValidationHelper.validLengthString(appMenu.Name, 64, "El nombre del menú solo puede tener 64 caracteres");
      ValidationHelper.validateIsNotEmpty(appMenu.Name, "Debe ingresar un nombre para el menú");

      if (appMenu.Description) {
        ValidationHelper.validLengthString(appMenu.Description, 128, "La descripción del menú solo puede tener 128 caracteres");
      }

      ValidationHelper.validateIsNotEmpty(appMenu.IsMenuDad, "Debe seleccionar si es menú padre");

      if (appMenu.IsMenuDad === 'N') {
        ValidationHelper.validateIsNotEmpty(appMenu.MenuDadCod, "Debe seleccionar un menú padre");
        ValidationHelper.validLengthString(appMenu.MenuDadCod, 8, "El código del menú padre solo puede tener 8 caracteres");
      }

      return true;
    } catch (e: any) {
      this.toastrService.error(e.message);
      return false;
    }
  }

  async existsMenuCod(): Promise<boolean> {
    const rpt: ResponseWsDto = await this.appMenuService.FindById(this.txtMenuCod.nativeElement.value);

    if (!rpt.ErrorStatus && rpt.Data) {
      const menu: AppMenuEntity = rpt.Data;
      return menu.MenuCod === this.txtMenuCod.nativeElement.value;
    }

    return false;
  }

  async checkMenuCodExistence() {
    if (!this.MenuCod && this.txtMenuCod.nativeElement.value) {
      const exists = await this.existsMenuCod();
      if (exists) {
        this.toastrService.warning("El código de menú ya existe.", "Validación");
        this.txtMenuCod.nativeElement.value = "";
        this.txtMenuCod.nativeElement.focus();
      }
    }
  }

}
