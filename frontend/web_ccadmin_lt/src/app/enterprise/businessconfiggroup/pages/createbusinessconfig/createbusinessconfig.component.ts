import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { ValidationHelper } from 'src/app/enterprise/shared/helper/ValidationHelper';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { BusinessConfigEntity } from 'src/app/enterprise/shared/model/entity/BusinessConfigEntity';
import { BusinessConfigGroupEntity } from '../../model/entity/BusinessConfigGroupEntity';
import { BusinessConfigService } from '../../service/BusinessConfigService';

interface BusinessConfigDynamicField {
  column: string;
  nameKey: string;
  technicalKey: string;
  label: string;
  key: string;
  type: 'text' | 'number';
}

@Component({
  selector: 'app-createbusinessconfig',
  templateUrl: './createbusinessconfig.component.html'
})
export class CreatebusinessconfigComponent implements OnInit {

  GroupCod: string = "";
  ConfigCorr: number = 0;
  ConfigCorrNext: number = 0;
  businessConfigGroup: BusinessConfigGroupEntity = new BusinessConfigGroupEntity();
  businessConfig: BusinessConfigEntity = new BusinessConfigEntity();
  businessConfigList: BusinessConfigEntity[] = [];
  dynamicFields: BusinessConfigDynamicField[] = [];

  private readonly fieldMap: BusinessConfigDynamicField[] = [
    { column: "ConfigCod", nameKey: "ConfigCodName", technicalKey: "ConfigCodKey", label: "", key: "", type: "text" },
    { column: "ConfigVal", nameKey: "ConfigValName", technicalKey: "ConfigValKey", label: "", key: "", type: "text" },
    { column: "ConfigName", nameKey: "ConfigNameName", technicalKey: "ConfigNameKey", label: "", key: "", type: "text" },
    { column: "ConfigDesc", nameKey: "ConfigDescName", technicalKey: "ConfigDescKey", label: "", key: "", type: "text" },
    { column: "Str1Config", nameKey: "Str1ConfigName", technicalKey: "Str1ConfigKey", label: "", key: "", type: "text" },
    { column: "Str2Config", nameKey: "Str2ConfigName", technicalKey: "Str2ConfigKey", label: "", key: "", type: "text" },
    { column: "Str3Config", nameKey: "Str3ConfigName", technicalKey: "Str3ConfigKey", label: "", key: "", type: "text" },
    { column: "Str4Config", nameKey: "Str4ConfigName", technicalKey: "Str4ConfigKey", label: "", key: "", type: "text" },
    { column: "Num1Config", nameKey: "Num1ConfigName", technicalKey: "Num1ConfigKey", label: "", key: "", type: "number" },
    { column: "Num2Config", nameKey: "Num2ConfigName", technicalKey: "Num2ConfigKey", label: "", key: "", type: "number" },
    { column: "Num3Config", nameKey: "Num3ConfigName", technicalKey: "Num3ConfigKey", label: "", key: "", type: "number" },
    { column: "Num4Config", nameKey: "Num4ConfigName", technicalKey: "Num4ConfigKey", label: "", key: "", type: "number" },
    { column: "Dcm1Config", nameKey: "Dcm1ConfigName", technicalKey: "Dcm1ConfigKey", label: "", key: "", type: "number" },
    { column: "Dcm2Config", nameKey: "Dcm2ConfigName", technicalKey: "Dcm2ConfigKey", label: "", key: "", type: "number" },
    { column: "Dcm3Config", nameKey: "Dcm3ConfigName", technicalKey: "Dcm3ConfigKey", label: "", key: "", type: "number" },
    { column: "Dcm4Config", nameKey: "Dcm4ConfigName", technicalKey: "Dcm4ConfigKey", label: "", key: "", type: "number" },
    { column: "Sta1Config", nameKey: "Sta1ConfigName", technicalKey: "Sta1ConfigKey", label: "", key: "", type: "text" },
    { column: "Sta2Config", nameKey: "Sta2ConfigName", technicalKey: "Sta2ConfigKey", label: "", key: "", type: "text" },
    { column: "Sta3Config", nameKey: "Sta3ConfigName", technicalKey: "Sta3ConfigKey", label: "", key: "", type: "text" },
    { column: "Sta4Config", nameKey: "Sta4ConfigName", technicalKey: "Sta4ConfigKey", label: "", key: "", type: "text" }
  ];

  constructor(
    private businessConfigService: BusinessConfigService,
    private router: Router,
    private toastrService: ToastrService
  ) {
    const urlTree: any = this.router.parseUrl(this.router.url);
    this.GroupCod = urlTree.queryParams['GroupCod'] ?? "";
    this.ConfigCorr = Number(urlTree.queryParams['ConfigCorr'] ?? 0);
  }

  ngOnInit(): void {
    this.loadPage();
  }

  async loadPage(): Promise<void> {
    await this.FindDataForm();
  }

  async FindDataForm(): Promise<void> {
    const rpt: ResponseWsDto = await this.businessConfigService.findDataForm(this.GroupCod);

    if (!rpt.ErrorStatus) {
      this.businessConfigGroup = rpt.DataAdditional?.find(e => e.Name === "businessConfigGroup")?.Data
        ?? rpt.DataAdditional?.find(e => e.Name === "BusinessConfigGroup")?.Data
        ?? new BusinessConfigGroupEntity();
      this.businessConfigList = rpt.DataAdditional?.find(e => e.Name === "businessConfigList")?.Data ?? [];
      this.ConfigCorrNext = rpt.DataAdditional?.find(e => e.Name === "ConfigCorrNext")?.Data ?? 0;
      this.dynamicFields = this.resolveDynamicFields();
      this.loadBusinessConfigSelected();
      this.prepareBusinessConfig();
    }
  }

  async Save(): Promise<void> {
    this.prepareBusinessConfig();

    if (!this.validate()) return;

    try {
      const rpt: ResponseWsDto = await this.businessConfigService.save(this.businessConfig);
      if (rpt.ErrorStatus) {
        this.toastrService.error(this.getResponseMessage(rpt, "No se pudo guardar la configuracion"));
        return;
      }

      this.toastrService.success("Configuracion guardada");
      this.ConfigCorr = 0;
      this.businessConfig = new BusinessConfigEntity();
      await this.FindDataForm();
    } catch (e: any) {
      this.toastrService.error(e?.message ?? "No se pudo guardar la configuracion");
    }
  }

  validate(): boolean {
    try {
      ValidationHelper.validateIsNotEmpty(this.businessConfig.GroupCod, "Debe seleccionar un grupo");
      ValidationHelper.validNumber(this.businessConfig.GroupId, null, 1, "El grupo debe tener un id valido");

      if (this.isFieldEnabled("ConfigCod")) {
        ValidationHelper.validateIsNotEmpty(this.businessConfig.ConfigCod, "Debe ingresar el codigo de configuracion");
      }

      return true;
    } catch (e: any) {
      this.toastrService.error(e.message);
      return false;
    }
  }

  goBack(): void {
    this.router.navigate(['/enterprise/businessconfiggroup/pages/listbusinessconfiggroup'], { queryParams: { GroupCod: this.GroupCod } });
  }

  async enable(item: BusinessConfigEntity): Promise<void> {
    try {
      const rpt: ResponseWsDto = await this.businessConfigService.enable(item);
      if (rpt.ErrorStatus) {
        this.toastrService.error(this.getResponseMessage(rpt, "No se pudo activar la configuracion"));
        return;
      }

      this.toastrService.success("Configuracion activada");
      await this.FindDataForm();
    } catch (e: any) {
      this.toastrService.error(e?.message ?? "No se pudo activar la configuracion");
    }
  }

  async disable(item: BusinessConfigEntity): Promise<void> {
    try {
      const rpt: ResponseWsDto = await this.businessConfigService.disable(item);
      if (rpt.ErrorStatus) {
        this.toastrService.error(this.getResponseMessage(rpt, "No se pudo inactivar la configuracion"));
        return;
      }

      this.toastrService.success("Configuracion inactivada");
      await this.FindDataForm();
    } catch (e: any) {
      this.toastrService.error(e?.message ?? "No se pudo inactivar la configuracion");
    }
  }

  private resolveDynamicFields(): BusinessConfigDynamicField[] {
    return this.fieldMap
      .map(field => {
        const name = this.businessConfigGroup[field.nameKey];
        const key = this.businessConfigGroup[field.technicalKey];

        return {
          ...field,
          label: this.getText(name) || this.getText(key) || field.column,
          key: this.getText(key)
        };
      })
      .filter(field => this.hasText(this.businessConfigGroup[field.nameKey]) || this.hasText(this.businessConfigGroup[field.technicalKey]));
  }

  private prepareBusinessConfig(): void {

    this.businessConfig.GroupId = this.businessConfigGroup.GroupId;
    this.businessConfig.GroupCod = this.businessConfigGroup.GroupCod;

    this.fieldMap.forEach(field => {
      if (!this.isFieldEnabled(field.column)) {
        this.businessConfig[field.column] = null;
      }
    });

    if(this.businessConfig.ConfigCorr === 0){
      this.businessConfig.ConfigCorr = this.ConfigCorrNext;
    }

    if (!this.businessConfig.Status) {
      this.businessConfig.Status = "A";
    }
  }

  private isFieldEnabled(column: string): boolean {
    return this.dynamicFields.some(e => e.column === column);
  }

  private hasText(value: any): boolean {
    return this.getText(value) !== "";
  }

  private getText(value: any): string {
    if (value === null || value === undefined) return "";
    return String(value).trim();
  }

  private loadBusinessConfigSelected(): void {
    if (this.ConfigCorr > 0) {
      const item = this.businessConfigList.find(e => e.ConfigCorr === this.ConfigCorr);
      this.businessConfig = item ? { ...item } as BusinessConfigEntity : new BusinessConfigEntity();
      return;
    }

    this.businessConfig = new BusinessConfigEntity();
  }

  private getResponseMessage(rpt: ResponseWsDto, defaultMessage: string): string {
    return this.getText(rpt?.Message) || defaultMessage;
  }
}
