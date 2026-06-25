import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { AppProfileService } from '../../service/appprofile.service';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { AppProfileEntity } from '../../model/entity/AppProfileEntity';
import { Router } from '@angular/router';
import { AppMenuStructureDto } from '../../model/dto/AppMenuStructureDto';
import { AppMenuEntity } from '../../../menu/model/entity/AppMenuEntity';
import { ProfileMenuEntity } from '../../model/entity/ProfileMenuEntity';

@Component({
  selector: 'app-createprofile',
  templateUrl: './createprofile.component.html'
})
export class CreateprofileComponent implements OnInit {

  @ViewChild('txtProfileCod') txtProfileCod!: ElementRef<HTMLSelectElement>;
  @ViewChild('txtName') txtName!: ElementRef<HTMLSelectElement>;
  @ViewChild('txtDescription') txtDescription!: ElementRef<HTMLSelectElement>;

  AppProfile : AppProfileEntity = new AppProfileEntity();
  AppMenuStructure : AppMenuStructureDto[] = [];
  AppMenuTable : AppMenuEntity[] = [];

  constructor(
    private appProfileService : AppProfileService,
    private router: Router
  ) 
  { 
    let urlTree : any = this.router.parseUrl(this.router.url);
    this.AppProfile.ProfileCod =  urlTree.queryParams['ProfileCod'];
    this.findDataForm(this.AppProfile.ProfileCod);
  }

  ngOnInit(): void {
  }

  async findDataForm(ProfileCod : string)
  {
    const rpt : ResponseWsDto = await this.appProfileService.findDataForm(ProfileCod);

    if( !rpt.Status )
    {
      this.AppProfile = rpt.DataAdditional.find( e => e.Name === "Profile" )?.Data;
      this.AppMenuStructure = rpt.DataAdditional.find( e => e.Name === "AppMenuStructure" )?.Data;

      setTimeout(() => {this.loadingForm(this.AppProfile);}, 100);
      this.loadingTable(this.AppMenuStructure);
      
    }
  }

  loadingTable(AppMenuStructure : AppMenuStructureDto[])
  {
      for(let item of AppMenuStructure)
      {
          this.AppMenuTable.push( item.MenuDad );

          for(let subitem of item.MenuChildList)
          {
            this.AppMenuTable.push( subitem );
          }
      }
  }

  loadingForm( AppProfile : AppProfileEntity )
  {
    if(!AppProfile) return;
    this.txtProfileCod.nativeElement.value = AppProfile.ProfileCod;
    this.txtName.nativeElement.value = AppProfile.Name;
    this.txtDescription.nativeElement.value = AppProfile.Description;
  }

   async save()
  {
    if(!this.AppProfile) this.AppProfile = new AppProfileEntity();

    this.AppProfile.ProfileCod = this.txtProfileCod.nativeElement.value;
    this.AppProfile.Name = this.txtName.nativeElement.value;
    this.AppProfile.Description = this.txtDescription.nativeElement.value;

    for(let permission of this.AppProfile.permissionsList)
    {
        permission.ProfileCod = this.AppProfile.ProfileCod;
    }

    const rpt = await this.appProfileService.save(this.AppProfile);
  }

  CheckedMenu(AppMenu : AppMenuEntity)
  {
      if(!this.AppProfile) this.AppProfile = new AppProfileEntity();

      if( this.AppProfile.permissionsList.find( e => e.MenuCod === AppMenu.MenuCod ) )
      {
        this.AppProfile.permissionsList = this.AppProfile.permissionsList.filter( e => e.MenuCod !== AppMenu.MenuCod );
      }
      else
      {
        let ProfileMenuNew : ProfileMenuEntity = new ProfileMenuEntity();

        ProfileMenuNew.MenuCod = AppMenu.MenuCod;
        ProfileMenuNew.ProfileCod = this.txtProfileCod.nativeElement.value;

        this.AppProfile.permissionsList.push( 
          ProfileMenuNew
        );
      }
  }

  IsChecked(AppMenu : AppMenuEntity) :boolean
  {
    if(!this.AppProfile) return false;

      const IsChecked : boolean = ( this.AppProfile.permissionsList.filter( e => e.MenuCod === AppMenu.MenuCod ).length > 0 );

      return IsChecked;
  }

}
