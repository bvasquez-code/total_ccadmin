import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { ValidationHelper } from 'src/app/enterprise/shared/helper/ValidationHelper';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { BusinessConfigGroupEntity } from '../../model/entity/BusinessConfigGroupEntity';
import { BusinessConfigGroupService } from '../../service/BusinessConfigGroupService';

type FieldType = 'text' | 'number' | 'textarea';

interface BusinessConfigGroupField {
  key: string;
  label: string;
  maxLength?: number;
  type?: FieldType;
  readonly?: boolean;
}

interface BusinessConfigGroupSection {
  title: string;
  fields: BusinessConfigGroupField[];
  hidden?: boolean;
}

const FIXED_FIELDS: { [key: string]: string } = {
  GroupIdName: "Identificador del grupo",
  GroupIdKey: "groupId",
  GroupCodName: "Código del grupo",
  GroupCodKey: "groupCode",
  ConfigCorrName: "Correlativo de configuración",
  ConfigCorrKey: "configOrder",
  CreationUserName: "Usuario de creación",
  CreationUserKey: "creationUser",
  CreationDateName: "Fecha de creación",
  CreationDateKey: "creationDate",
  ModifyUserName: "",
  ModifyUserKey: "",
  ModifyDateName: "Fecha de modificación",
  ModifyDateKey: "modifyDate",
  StatusName: "Estado",
  StatusKey: "status"
};

@Component({
  selector: 'app-createbusinessconfiggroup',
  templateUrl: './createbusinessconfiggroup.component.html'
})
export class CreatebusinessconfiggroupComponent implements OnInit {

  GroupCod: string = "";
  businessConfigGroup: BusinessConfigGroupEntity = new BusinessConfigGroupEntity();
  savedBusinessConfigGroup: BusinessConfigGroupEntity = new BusinessConfigGroupEntity();
  txtGroupCodReadonly: boolean = false;
  isHeaderCollapsed: boolean = false;

  sections: BusinessConfigGroupSection[] = [
    {
      title: "Datos generales",
      fields: [
        { key: "GroupId", label: "Id", type: "number" },
        { key: "GroupCod", label: "Codigo", maxLength: 64 },
        { key: "GroupName", label: "Nombre", maxLength: 128 },
        { key: "GroupDesc", label: "Descripcion", maxLength: 500, type: "textarea" }
      ]
    },
    {
      title: "Campos base",
      fields: [
        { key: "GroupIdName", label: "Nombre GroupId", maxLength: 128 },
        { key: "GroupIdKey", label: "Key GroupId", maxLength: 64 },
        { key: "GroupCodName", label: "Nombre GroupCod", maxLength: 128 },
        { key: "GroupCodKey", label: "Key GroupCod", maxLength: 64 },
        { key: "ConfigCorrName", label: "Nombre ConfigCorr", maxLength: 128 },
        { key: "ConfigCorrKey", label: "Key ConfigCorr", maxLength: 64 },
        { key: "ConfigCodName", label: "Nombre ConfigCod", maxLength: 128 },
        { key: "ConfigCodKey", label: "Key ConfigCod", maxLength: 64 },
        { key: "ConfigValName", label: "Nombre ConfigVal", maxLength: 128 },
        { key: "ConfigValKey", label: "Key ConfigVal", maxLength: 64 },
        { key: "ConfigNameName", label: "Nombre ConfigName", maxLength: 128 },
        { key: "ConfigNameKey", label: "Key ConfigName", maxLength: 64 },
        { key: "ConfigDescName", label: "Nombre ConfigDesc", maxLength: 128 },
        { key: "ConfigDescKey", label: "Key ConfigDesc", maxLength: 64 }
      ]
    },
    {
      title: "Campos texto",
      fields: this.buildPairFields("Str", "Texto")
    },
    {
      title: "Campos numericos",
      fields: this.buildPairFields("Num", "Numero")
    },
    {
      title: "Campos decimales",
      fields: this.buildPairFields("Dcm", "Decimal")
    },
    {
      title: "Campos estado",
      fields: this.buildPairFields("Sta", "Estado")
    },
    {
      title: "Auditoria",
      fields: [
        { key: "CreationUserName", label: "Nombre CreationUser", maxLength: 128 },
        { key: "CreationUserKey", label: "Key CreationUser", maxLength: 64 },
        { key: "CreationDateName", label: "Nombre CreationDate", maxLength: 128 },
        { key: "CreationDateKey", label: "Key CreationDate", maxLength: 64 },
        { key: "ModifyUserName", label: "Nombre ModifyUser", maxLength: 128 },
        { key: "ModifyUserKey", label: "Key ModifyUser", maxLength: 64 },
        { key: "ModifyDateName", label: "Nombre ModifyDate", maxLength: 128 },
        { key: "ModifyDateKey", label: "Key ModifyDate", maxLength: 64 },
        { key: "StatusName", label: "Nombre Status", maxLength: 128 },
        { key: "StatusKey", label: "Key Status", maxLength: 64 }
      ],
      hidden: true
    }
  ];

  constructor(
    private businessConfigGroupService: BusinessConfigGroupService,
    private router: Router,
    private toastrService: ToastrService
  ) {
    this.GetParamUrl(this.router);
  }

  ngOnInit(): void {
    this.FindDataForm(this.GroupCod);
  }

  get visibleSections(): BusinessConfigGroupSection[] {
    return this.sections.filter(e => !e.hidden);
  }

  GetParamUrl(router: Router): void {
    const urlTree: any = router.parseUrl(this.router.url);
    this.GroupCod = urlTree.queryParams['GroupCod'] ?? "";
  }

  async FindDataForm(GroupCod: string): Promise<void> {
    const rpt: ResponseWsDto = await this.businessConfigGroupService.findDataForm(GroupCod);

    if (!rpt.ErrorStatus) {
      const item = rpt.DataAdditional?.find(e => e.Name === "businessConfigGroup")?.Data
        ?? rpt.DataAdditional?.find(e => e.Name === "BusinessConfigGroup")?.Data
        ?? rpt.Data;

      if (item) this.businessConfigGroup = item;
      this.applyFixedFields();
      if (GroupCod !== "") this.txtGroupCodReadonly = true;
    }
  }

  async Save(): Promise<void> {
    if (!this.businessConfigGroup) this.businessConfigGroup = new BusinessConfigGroupEntity();
    this.applyFixedFields();

    if (!this.validate(this.businessConfigGroup)) return;

    const rpt: ResponseWsDto = await this.businessConfigGroupService.save(this.businessConfigGroup);

    if (!rpt.ErrorStatus) {
      this.toastrService.success("Operacion realizada con exito.");
      this.savedBusinessConfigGroup = rpt.Data ?? this.businessConfigGroup;
      this.businessConfigGroup = this.savedBusinessConfigGroup ?? new BusinessConfigGroupEntity();
      this.GroupCod = this.businessConfigGroup.GroupCod;
      this.txtGroupCodReadonly = true;
      this.isHeaderCollapsed = true;
    }
  }

  validate(entity: BusinessConfigGroupEntity): boolean {
    try {
      ValidationHelper.validNumber(entity.GroupId, null, 1, "Debe ingresar un id valido");
      ValidationHelper.validateIsNotEmpty(entity.GroupCod, "Debe ingresar un codigo de grupo");
      ValidationHelper.validLengthString(entity.GroupCod, 64, "El codigo de grupo solo puede tener 64 caracteres");
      ValidationHelper.validLengthString(entity.GroupName, 128, "El nombre de grupo solo puede tener 128 caracteres");
      ValidationHelper.validLengthString(entity.GroupDesc, 500, "La descripcion solo puede tener 500 caracteres");

      for (const section of this.sections) {
        for (const field of section.fields) {
          if (!field.maxLength) continue;
          ValidationHelper.validLengthString(entity[field.key] ?? "", field.maxLength, `${field.label} solo puede tener ${field.maxLength} caracteres`);
        }
      }

      return true;
    } catch (e: any) {
      this.toastrService.error(e.message);
      return false;
    }
  }

  isReadonly(field: BusinessConfigGroupField): boolean {
    return ((field.key === "GroupCod" || field.key === "GroupId") && this.txtGroupCodReadonly) || this.isFixedField(field.key);
  }

  inputType(field: BusinessConfigGroupField): string {
    return field.type === "number" ? "number" : "text";
  }

  toggleHeaderForm(): void {
    this.isHeaderCollapsed = !this.isHeaderCollapsed;
  }

  configureValues(): void {
    this.router.navigate(['/enterprise/businessconfiggroup/pages/createbusinessconfig'], { queryParams: { GroupCod: this.businessConfigGroup.GroupCod } });
  }

  private buildPairFields(prefix: string, label: string): BusinessConfigGroupField[] {
    const fields: BusinessConfigGroupField[] = [];

    for (let i = 1; i <= 4; i++) {
      fields.push({ key: `${prefix}${i}ConfigName`, label: `Nombre ${label} ${i}`, maxLength: 128 });
      fields.push({ key: `${prefix}${i}ConfigKey`, label: `Key ${label} ${i}`, maxLength: 64 });
    }

    return fields;
  }

  isFixedField(key: string): boolean {
    return Object.prototype.hasOwnProperty.call(FIXED_FIELDS, key);
  }

  private applyFixedFields(): void {
    if (!this.businessConfigGroup) this.businessConfigGroup = new BusinessConfigGroupEntity();

    Object.keys(FIXED_FIELDS).forEach(key => {
      this.businessConfigGroup[key] = FIXED_FIELDS[key];
    });
  }
}
