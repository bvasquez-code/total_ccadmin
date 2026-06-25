import { Component, ElementRef, EventEmitter, Input, Output, ViewChild, OnInit } from '@angular/core';
import { ClientService } from '../../service/client.service';
import { ClientEntity } from '../../model/entity/ClientEntity';
import { ResponseWsDto } from '../../../shared/model/dto/ResponseWsDto';
import { NonNullableFormBuilder } from '@angular/forms';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { ValidationHelper } from 'src/app/enterprise/shared/helper/ValidationHelper';
import { PersonService } from 'src/app/enterprise/person/service/person.service';
import { PersonEntity } from 'src/app/enterprise/person/model/entity/PersonEntity';

@Component({
  selector: 'app-createclient',
  templateUrl: './createclient.component.html'
})
export class CreateclientComponent implements OnInit{

  @Input() InputDocumentNum : string = ""; 
  @Input() InputDocumentType : string = "";
  @Input() InvokeType : string = "form";
  @Output() ResultForm = new EventEmitter<object>();

  @ViewChild('cboPersonType') cboPersonType!: ElementRef<HTMLSelectElement>;
  @ViewChild('cboDocumentType') cboDocumentType!: ElementRef<HTMLSelectElement>;
  @ViewChild('txtDocumentNum') txtDocumentNum!: ElementRef<HTMLInputElement>;
  @ViewChild('txtNames') txtNames?: ElementRef<HTMLInputElement>;
  @ViewChild('txtLastNames') txtLastNames?: ElementRef<HTMLInputElement>;
  @ViewChild('txtCommercialName') txtCommercialName?: ElementRef<HTMLInputElement>;
  @ViewChild('txtBusinessName') txtBusinessName?: ElementRef<HTMLInputElement>;
  @ViewChild('txtAddress') txtAddress?: ElementRef<HTMLInputElement>;
  @ViewChild('txtCellPhone') txtCellPhone!: ElementRef<HTMLInputElement>;
  @ViewChild('txtEmail') txtEmail!: ElementRef<HTMLInputElement>;
  @ViewChild('txtPhone') txtPhone!: ElementRef<HTMLInputElement>;

  Client : ClientEntity = new ClientEntity();
  IsLegalPerson: boolean = false;
  IsEdit: boolean = false;
  ExistingRegisterMessage: string = "";
  ExistingRegisterUrl: string = "";

  public constructor(
    private clientService : ClientService,
    private personService : PersonService,
    private router: Router,
    private toastrService: ToastrService
  )
  {
    let urlTree : any = this.router.parseUrl(this.router.url);
    this.Client.ClientCod =  urlTree.queryParams['ClientCod'];
    this.IsEdit = !!this.Client.ClientCod;
    if(this.IsEdit) this.findDataForm(this.Client.ClientCod);

    setTimeout(() => {this.loadingModal();}, 100);
  }


  ngOnInit(): void {
    
  }

  
  async findDataForm(ClientCod : string)
  {
    const rpt : ResponseWsDto = await this.clientService.findDataForm(ClientCod);

    if( !rpt.Status )
    {
      this.Client = rpt.DataAdditional.find( e => e.Name === "Client" )?.Data;

      setTimeout(() => {this.loadingForm(this.Client);}, 100);
      
    }
  }

  async save()
  {
    if(!this.Client) this.Client = new ClientEntity();

      this.Client.Person.PersonType = this.cboPersonType.nativeElement.value;
      this.Client.Person.DocumentType = this.cboDocumentType.nativeElement.value;
      this.Client.Person.DocumentNum = this.txtDocumentNum.nativeElement.value;
      this.Client.Person.Names = this.IsLegalPerson ? "-" : (this.txtNames?.nativeElement.value || "");
      this.Client.Person.LastNames = this.IsLegalPerson ? "-" : (this.txtLastNames?.nativeElement.value || "");
      this.Client.Person.CommercialName = this.IsLegalPerson ? (this.txtCommercialName?.nativeElement.value || "") : "";
      this.Client.Person.BusinessName = this.IsLegalPerson ? (this.txtBusinessName?.nativeElement.value || "") : "";
      this.Client.Person.Address = this.IsLegalPerson ? (this.txtAddress?.nativeElement.value || "") : "";
      this.Client.Person.CellPhone = this.txtCellPhone.nativeElement.value;
      this.Client.Person.Email = this.txtEmail.nativeElement.value;
      this.Client.Person.Phone = this.txtPhone.nativeElement.value;

      if (this.ExistingRegisterMessage) return;

      if (!this.validate(this.Client)) return;
      if (!this.IsEdit && await this.validateExistingRegister(this.Client.Person.DocumentType, this.Client.Person.DocumentNum)) return;

      const rpt : ResponseWsDto = await this.clientService.Save(this.Client);

      if( !rpt.ErrorStatus )
      {
        if( this.InvokeType === "modal" ){
          this.Client = rpt.Data;
          this.EmitResultForm(this.Client);
        }else{
          this.toastrService.success(rpt.Message);
          this.router.navigate(['/enterprise/client/pages/listclient']);
        }
        
      }
  }

  validate(client: ClientEntity): boolean {
    try {
      if (client.ClientCod) {
        ValidationHelper.validLengthString(client.ClientCod, 16, "El codigo de cliente solo puede tener 16 caracteres");
      }

      if (client.PersonCod) {
        ValidationHelper.validLengthString(client.PersonCod, 16, "El codigo de persona solo puede tener 16 caracteres");
      }

      ValidationHelper.validateIsNotEmpty(client.Person.PersonType, "Debe seleccionar un tipo de persona");
      ValidationHelper.validLengthString(client.Person.PersonType, 2, "El tipo de persona solo puede tener 2 caracteres");

      ValidationHelper.validateIsNotEmpty(client.Person.DocumentType, "Debe seleccionar un tipo de documento");
      ValidationHelper.validLengthString(client.Person.DocumentType, 2, "El tipo de documento solo puede tener 2 caracteres");

      ValidationHelper.validLengthString(client.Person.DocumentNum, 16, "El numero de documento solo puede tener 16 caracteres");
      ValidationHelper.validateIsNotEmpty(client.Person.DocumentNum, "Debe ingresar un numero de documento");

      if (client.Person.DocumentType === "01" && client.Person.DocumentNum.length !== 8) {
        throw new Error("El numero de documento DNI debe tener 8 caracteres");
      }

      if (client.Person.DocumentType === "04" && client.Person.DocumentNum.length < 9) {
        throw new Error("El numero de carnet de extranjeria debe tener como minimo 9 caracteres");
      }

      if (client.Person.DocumentType === "06" && client.Person.DocumentNum.length !== 11) {
        throw new Error("El numero de RUC debe tener 11 caracteres");
      }

      if (client.Person.PersonType === "04") {
        ValidationHelper.validateIsNotEmpty(client.Person.CommercialName, "Debe ingresar el nombre comercial");
        ValidationHelper.validLengthString(client.Person.CommercialName, 128, "El nombre comercial solo puede tener 128 caracteres");

        ValidationHelper.validateIsNotEmpty(client.Person.BusinessName, "Debe ingresar la razon social");
        ValidationHelper.validLengthString(client.Person.BusinessName, 128, "La razon social solo puede tener 128 caracteres");

        ValidationHelper.validateIsNotEmpty(client.Person.Address, "Debe ingresar la direccion");
        ValidationHelper.validLengthString(client.Person.Address, 256, "La direccion solo puede tener 256 caracteres");
      } else {
        ValidationHelper.validLengthString(client.Person.Names, 128, "Los nombres solo pueden tener 128 caracteres");
        ValidationHelper.validateIsNotEmpty(client.Person.Names, "Debe ingresar los nombres");
        ValidationHelper.isValidString(client.Person.Names, "Los nombres solo deben contener letras", /^[a-zA-Z\u00C0-\u017F\s]+$/);

        ValidationHelper.validLengthString(client.Person.LastNames, 128, "Los apellidos solo pueden tener 128 caracteres");
        ValidationHelper.validateIsNotEmpty(client.Person.LastNames, "Debe ingresar los apellidos");
        ValidationHelper.isValidString(client.Person.LastNames, "Los apellidos solo deben contener letras", /^[a-zA-Z\u00C0-\u017F\s]+$/);
      }

      if (client.Person.CellPhone) {
        ValidationHelper.validLengthString(client.Person.CellPhone, 20, "El celular solo puede tener 20 caracteres");
        ValidationHelper.isValidString(client.Person.CellPhone, "El celular solo debe contener numeros", /^[0-9]+$/);
      }

      if (client.Person.Email) {
        ValidationHelper.validLengthString(client.Person.Email, 32, "El email solo puede tener 32 caracteres");
      }

      if (client.Person.Phone) {
        ValidationHelper.validLengthString(client.Person.Phone, 20, "El telefono solo puede tener 20 caracteres");
        ValidationHelper.isValidString(client.Person.Phone, "El telefono solo debe contener numeros", /^[0-9]+$/);
      }

      return true;
    } catch (e: any) {
      this.toastrService.error(e.message);
      return false;
    }
  }

  validateKeypress(event: KeyboardEvent, id: string) {
    try {
      if (id === "txtNames" || id === "txtLastNames") {
        ValidationHelper.isValidString(event.key.toString(), "Error", /^[a-zA-Z\u00C0-\u017F\s]$/);
      }

      if (id === "txtCellPhone" || id === "txtPhone") {
        ValidationHelper.isValidString(event.key.toString(), "Error", /^[0-9]$/);
      }
    } catch (e: any) {
      event.preventDefault();
    }
  }

  async findByDocumentNum()
  {
    await this.findPersonByDocumentNum();
  }

  async findPersonByDocumentNum()
  {
    let DocumentType : string = "";
    let DocumentNum : string = "";

    if (this.IsEdit) return;

    this.clearExistingRegisterMessage();

    DocumentType = this.cboDocumentType.nativeElement.value;
    DocumentNum = this.txtDocumentNum.nativeElement.value;

    if( DocumentType === "01" && String(DocumentNum).length != 8 ) return;
    if( DocumentType === "04" && String(DocumentNum).length < 9 ) return;
    if( DocumentType === "06" && String(DocumentNum).length != 11 ) return;

    if (await this.validateExistingRegister(DocumentType, DocumentNum)) return;

    const rpt : ResponseWsDto = await this.personService.findByDocumentNum(DocumentType,DocumentNum);

    if( !rpt.ErrorStatus )
    {
      if(rpt.Data != NonNullableFormBuilder && rpt.Data) {
        this.Client.Person = rpt.Data;
        this.loadingPerson(this.Client.Person);
      }
    }

  }

  clearExistingRegisterMessage()
  {
    this.ExistingRegisterMessage = "";
    this.ExistingRegisterUrl = "";
  }

  async validateExistingRegister(DocumentType: string, DocumentNum: string): Promise<boolean>
  {
    const rptClient : ResponseWsDto = await this.clientService.findByDocumentNum(DocumentType,DocumentNum);

    if( !rptClient.ErrorStatus && rptClient.Data && rptClient.Data.ClientCod )
    {
      this.ExistingRegisterMessage = "El cliente ya existe. No puede volver a crearlo; busquelo o editelo.";
      this.ExistingRegisterUrl = `/enterprise/client/pages/createclient?ClientCod=${rptClient.Data.ClientCod}`;
      return true;
    }

    return false;
  }

  loadingPerson(Person : PersonEntity)
  {
    this.cboPersonType.nativeElement.value = Person.PersonType;
    this.changePersonType();
    this.cboDocumentType.nativeElement.value = Person.DocumentType;
    this.txtDocumentNum.nativeElement.value = Person.DocumentNum;
    this.txtCellPhone.nativeElement.value = Person.CellPhone;
    this.txtEmail.nativeElement.value = Person.Email;
    this.txtPhone.nativeElement.value = Person.Phone;

    setTimeout(() => {
      if (this.IsLegalPerson) {
        if (this.txtCommercialName) this.txtCommercialName.nativeElement.value = Person.CommercialName;
        if (this.txtBusinessName) this.txtBusinessName.nativeElement.value = Person.BusinessName;
        if (this.txtAddress) this.txtAddress.nativeElement.value = Person.Address;
      } else {
        if (this.txtNames) this.txtNames.nativeElement.value = Person.Names;
        if (this.txtLastNames) this.txtLastNames.nativeElement.value = Person.LastNames;
      }
    }, 100);
  }

  loadingForm( Client : ClientEntity )
  {
    this.loadingPerson(Client.Person);
  }

  EmitResultForm(Client : ClientEntity)
  {
    this.ResultForm.emit(Client);
  }

  loadingModal()
  {
    if( this.InvokeType === "modal" )
    {
      if (this.InputDocumentType === "06") {
        this.cboPersonType.nativeElement.value = "04";
        this.changePersonType();
      }

      setTimeout(() => {
        this.txtDocumentNum.nativeElement.value = this.InputDocumentNum;
        this.cboDocumentType.nativeElement.value = this.InputDocumentType;
      }, 0);
    }
    
  }

  changePersonType()
  {
    this.IsLegalPerson = this.cboPersonType.nativeElement.value === "04";
    if (this.IsLegalPerson) {
      setTimeout(() => { this.cboDocumentType.nativeElement.value = "06"; }, 0);
    } else if (!this.IsEdit && this.cboDocumentType.nativeElement.value === "06") {
      this.cboDocumentType.nativeElement.value = "01";
    }
  }

}
