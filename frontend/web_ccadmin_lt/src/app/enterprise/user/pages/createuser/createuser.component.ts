import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { AppUserEntity } from '../../model/entity/AppUserEntity';
import { AppUserService } from '../../service/appuser.service';
import { Router } from '@angular/router';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { AppProfileEntity } from '../../model/entity/AppProfileEntity';
import { UserProfileEntity } from '../../model/entity/UserProfileEntity';
import { StoreEntity } from 'src/app/enterprise/shared/model/entity/StoreEntity';
import { UserStoreEntity } from '../../model/entity/UserStoreEntity';
import { ToastrService } from 'ngx-toastr';
import { ValidationHelper } from 'src/app/enterprise/shared/helper/ValidationHelper';
import { SearchDto } from 'src/app/enterprise/shared/model/dto/SearchDto';
import { ResponsePageSearch } from 'src/app/enterprise/shared/model/dto/ResponsePageSearch';

@Component({
  selector: 'app-createuser',
  templateUrl: './createuser.component.html'
})
export class CreateuserComponent implements OnInit {

  @ViewChild('cboDocumentType') cboDocumentType!: ElementRef<HTMLSelectElement>;
  @ViewChild('txtDocumentNum') txtDocumentNum!: ElementRef<HTMLInputElement>;
  @ViewChild('txtNames') txtNames!: ElementRef<HTMLInputElement>;
  @ViewChild('txtLastNames') txtLastNames!: ElementRef<HTMLInputElement>;
  @ViewChild('txtCellPhone') txtCellPhone!: ElementRef<HTMLInputElement>;
  @ViewChild('txtEmail') txtEmail!: ElementRef<HTMLInputElement>;
  @ViewChild('txtUserCod') txtUserCod!: ElementRef<HTMLInputElement>;
  @ViewChild('txtPassword') txtPassword!: ElementRef<HTMLInputElement>;

  AppUser: AppUserEntity = new AppUserEntity();
  ProfileList: AppProfileEntity[] = [];
  StoreList: StoreEntity[] = [];
  IsEditMode: boolean = false;

  private OriginalUserCod: string = '';

  constructor(
    private appUserService: AppUserService,
    private router: Router,
    private toastrService: ToastrService
  ) {

    let urlTree: any = this.router.parseUrl(this.router.url);
    this.OriginalUserCod = (urlTree.queryParams['UserCod'] || '').trim();
    this.IsEditMode = this.OriginalUserCod.length > 0;
    this.AppUser.UserCod = this.OriginalUserCod;
    this.findDataForm(this.AppUser.UserCod);

  }

  ngOnInit(): void {
  }

  async findDataForm(UserCod: string) {
    const rpt: ResponseWsDto = await this.appUserService.findDataForm(UserCod);

    if (!rpt.Status) {
      this.AppUser = rpt.DataAdditional.find(e => e.Name === "User")?.Data;
      this.ProfileList = rpt.DataAdditional.find(e => e.Name === "ProfileList")?.Data;
      this.StoreList = rpt.DataAdditional.find(e => e.Name === "StoreList")?.Data;

      setTimeout(() => { this.loadingForm(this.AppUser); }, 100);

    }
  }

  loadingForm(AppUser: AppUserEntity) {
    this.txtUserCod.nativeElement.value = AppUser.UserCod;
    this.txtPassword.nativeElement.value = AppUser.PasswordDecoded;

    this.cboDocumentType.nativeElement.value = AppUser.Person.DocumentType;
    this.txtDocumentNum.nativeElement.value = AppUser.Person.DocumentNum;
    this.txtNames.nativeElement.value = AppUser.Person.Names;
    this.txtLastNames.nativeElement.value = AppUser.Person.LastNames;
    this.txtCellPhone.nativeElement.value = AppUser.Person.CellPhone;
    this.txtEmail.nativeElement.value = AppUser.Person.Email;
  }

  async save() {
    if (!this.AppUser) this.AppUser = new AppUserEntity();

    this.AppUser.UserCod = (this.IsEditMode
      ? this.OriginalUserCod
      : this.txtUserCod.nativeElement.value).trim();
    this.AppUser.PasswordDecoded = this.txtPassword.nativeElement.value;

    this.AppUser.Person.DocumentType = this.cboDocumentType.nativeElement.value;
    this.AppUser.Person.DocumentNum = this.txtDocumentNum.nativeElement.value.trim();
    this.AppUser.Person.Names = this.txtNames.nativeElement.value;
    this.AppUser.Person.LastNames = this.txtLastNames.nativeElement.value;
    this.AppUser.Person.CellPhone = this.txtCellPhone.nativeElement.value;
    this.AppUser.Person.Email = this.txtEmail.nativeElement.value;

    if (!this.validate(this.AppUser)) return;

    let userWithSameDocument: AppUserEntity | null;
    let userWithSameCode: AppUserEntity | null = null;
    try {
      [userWithSameDocument, userWithSameCode] = await Promise.all([
        this.findUserWithDocumentNumber(this.AppUser.Person.DocumentNum),
        this.IsEditMode
          ? Promise.resolve(null)
          : this.findUserWithUserCode(this.AppUser.UserCod)
      ]);
    } catch (error: any) {
      this.toastrService.error(error.message || 'No se pudo validar el usuario');
      return;
    }
    if (userWithSameCode) {
      this.toastrService.error(`El usuario ${this.AppUser.UserCod} ya existe.`);
      this.txtUserCod.nativeElement.focus();
      return;
    }
    if (userWithSameDocument) {
      this.toastrService.error(
        `El número de documento ya está asignado al usuario ${userWithSameDocument.UserCod}.`
      );
      this.txtDocumentNum.nativeElement.focus();
      return;
    }

    const rpt: ResponseWsDto = await this.appUserService.save(this.AppUser);

    if (!rpt.ErrorStatus) {
      this.toastrService.success("Usuario guardado correctamente.");
      this.router.navigate(['/enterprise/user/pages/listuser']);
    }

  }

  private async findUserWithDocumentNumber(documentNumber: string): Promise<AppUserEntity | null> {
    const normalizedDocumentNumber = this.normalizeDocumentNumber(documentNumber);
    const userList = await this.findUsers(normalizedDocumentNumber);
    return userList.find(user =>
        this.normalizeUserCode(user.UserCod) !== this.normalizeUserCode(this.OriginalUserCod)
        && this.normalizeDocumentNumber(user.Person?.DocumentNum) === normalizedDocumentNumber
      ) || null;
  }

  private async findUserWithUserCode(userCode: string): Promise<AppUserEntity | null> {
    const normalizedUserCode = this.normalizeUserCode(userCode);
    const userList = await this.findUsers(normalizedUserCode);
    return userList.find(user =>
      this.normalizeUserCode(user.UserCod) === normalizedUserCode
    ) || null;
  }

  private async findUsers(query: string): Promise<AppUserEntity[]> {
    const search = new SearchDto();
    search.Page = 1;
    search.Query = query;

    const response = await this.appUserService.findAll(search);
    if (response.ErrorStatus) {
      throw new Error(response.Message || 'No se pudo validar el usuario');
    }

    const pageSearch = response.Data as ResponsePageSearch<AppUserEntity>;
    return pageSearch?.resultSearch || [];
  }

  private normalizeDocumentNumber(documentNumber: string | null | undefined): string {
    return (documentNumber || '').trim().toUpperCase();
  }

  private normalizeUserCode(userCode: string | null | undefined): string {
    return (userCode || '').trim().toUpperCase();
  }

  validate(appUser: AppUserEntity): boolean {
    try {
      ValidationHelper.validLengthString(appUser.UserCod, 16, "El código de usuario solo puede tener 16 caracteres");
      ValidationHelper.validateIsNotEmpty(appUser.UserCod, "Debe ingresar un código de usuario");

      if (appUser.PasswordDecoded) {
        ValidationHelper.validLengthString(appUser.PasswordDecoded, 100, "La contraseña solo puede tener 100 caracteres");
      }
      ValidationHelper.validateIsNotEmpty(appUser.PasswordDecoded, "Debe ingresar una contraseña");

      ValidationHelper.validateIsNotEmpty(appUser.Person.DocumentType, "Debe seleccionar un tipo de documento");
      ValidationHelper.validLengthString(appUser.Person.DocumentType, 2, "El tipo de documento solo puede tener 2 caracteres");

      ValidationHelper.validLengthString(appUser.Person.DocumentNum, 16, "El número de documento solo puede tener 16 caracteres");
      ValidationHelper.validateIsNotEmpty(appUser.Person.DocumentNum, "Debe ingresar un número de documento");

      ValidationHelper.validLengthString(appUser.Person.Names, 128, "Los nombres solo pueden tener 128 caracteres");
      ValidationHelper.validateIsNotEmpty(appUser.Person.Names, "Debe ingresar los nombres");

      ValidationHelper.validLengthString(appUser.Person.LastNames, 128, "Los apellidos solo pueden tener 128 caracteres");
      ValidationHelper.validateIsNotEmpty(appUser.Person.LastNames, "Debe ingresar los apellidos");

      if (appUser.Person.CellPhone) {
        ValidationHelper.validLengthString(appUser.Person.CellPhone, 20, "El celular solo puede tener 20 caracteres");
      }

      if (appUser.Person.Email) {
        ValidationHelper.validLengthString(appUser.Person.Email, 32, "El email solo puede tener 32 caracteres");
      }

      return true;
    } catch (e: any) {
      this.toastrService.error(e.message);
      return false;
    }
  }

  CheckedMenu(AppUser: AppProfileEntity) {
    if (!this.AppUser) this.AppUser = new AppUserEntity();

    if (this.AppUser.UserProfileList.find(e => e.ProfileCod === AppUser.ProfileCod)) {
      this.AppUser.UserProfileList = this.AppUser.UserProfileList.filter(e => e.ProfileCod !== AppUser.ProfileCod);
    }
    else {
      let UserProfile: UserProfileEntity = new UserProfileEntity();

      UserProfile.ProfileCod = AppUser.ProfileCod;
      UserProfile.UserCod = this.txtDocumentNum.nativeElement.value;

      this.AppUser.UserProfileList.push(
        UserProfile
      );
    }
  }

  IsChecked(AppUser: AppProfileEntity): boolean {
    if (!this.AppUser) return false;

    const IsChecked: boolean = (this.AppUser.UserProfileList.filter(e => e.ProfileCod === AppUser.ProfileCod).length > 0);

    return IsChecked;
  }

  CheckedStore(Store: StoreEntity) {
    if (!this.AppUser) this.AppUser = new AppUserEntity();

    if (this.AppUser.UserStoreList.find(e => e.StoreCod === Store.StoreCod)) {
      this.AppUser.UserStoreList = this.AppUser.UserStoreList.filter(e => e.StoreCod !== Store.StoreCod);
    }
    else {
      let UserStore: UserStoreEntity = new UserStoreEntity();

      UserStore.StoreCod = Store.StoreCod;
      UserStore.UserCod = this.txtUserCod.nativeElement.value;

      this.AppUser.UserStoreList.push(
        UserStore
      );
    }
  }

  IsCheckedStore(Store: StoreEntity): boolean {
    if (!this.AppUser) return false;

    const IsChecked: boolean = (this.AppUser.UserStoreList.filter(e => e.StoreCod === Store.StoreCod).length > 0);

    return IsChecked;
  }

}
