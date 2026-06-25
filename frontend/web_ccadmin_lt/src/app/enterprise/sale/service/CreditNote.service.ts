import { Injectable } from '@angular/core';
import { ApiService } from '../../compartido/service/api.service';
import { ICrudService } from '../../shared/interface/ICrudService';
import { CreditNoteHeadDto } from '../model/dto/CreditNoteHeadDto';
import { ResponseWsDto } from '../../shared/model/dto/ResponseWsDto';
import { SearchDto } from '../../shared/model/dto/SearchDto';
import { AppSetting } from 'src/app/config/app.setting';
import { CreditNoteRegisterDto } from '../model/dto/CreditNoteRegisterDto';
import { CreditNoteReturnPaymentRegisterDto } from '../model/dto/CreditNoteReturnPaymentRegisterDto';

@Injectable({
  providedIn: 'root'
})
export class CreditNoteService implements ICrudService<CreditNoteRegisterDto,string>{

constructor(private apiService: ApiService) { }


  async FindById(CreditNoteCod: string): Promise<ResponseWsDto> {
    
    let url: string = `${AppSetting.API}/api/v1/CreditNote/findById`;
    let RespuestaWS : ResponseWsDto;

    RespuestaWS = await this.apiService.ExecuteGetService(url,{CreditNoteCod : CreditNoteCod});

    return RespuestaWS;
  }

  async FindAll(Search: SearchDto): Promise<ResponseWsDto> {
    
    let url: string = `${AppSetting.API}/api/v1/CreditNote/findAll`;
    let RespuestaWS : ResponseWsDto;

    RespuestaWS = await this.apiService.ExecuteGetService(url,Search);

    return RespuestaWS;
  }

  async Save(Entity: CreditNoteRegisterDto): Promise<ResponseWsDto> {
    let url: string = `${AppSetting.API}/api/v1/CreditNote/save`;
    let RespuestaWS : ResponseWsDto;

    RespuestaWS = await this.apiService.ExecutePostService(url,Entity);

    return RespuestaWS;
  }

  async SaveReturnStock(Entity: CreditNoteRegisterDto): Promise<ResponseWsDto> {
    let url: string = `${AppSetting.API}/api/v1/CreditNote/saveReturnStock`;
    let RespuestaWS : ResponseWsDto;

    RespuestaWS = await this.apiService.ExecutePostService(url,Entity);

    return RespuestaWS;
  }

  async AddReturnPayment(payment : CreditNoteReturnPaymentRegisterDto): Promise<ResponseWsDto> {
    let url: string = `${AppSetting.API}/api/v1/CreditNote/addReturnPayment`;
    let RespuestaWS : ResponseWsDto;

    RespuestaWS = await this.apiService.ExecutePostService(url,payment);

    return RespuestaWS;
  }


  SaveAll(EntityList: CreditNoteRegisterDto[]): Promise<ResponseWsDto> {
    throw new Error('Method not implemented.');
  }

  FindAllById(IdList: string[]): Promise<ResponseWsDto> {
    throw new Error('Method not implemented.');
  }

  async CreateCode(): Promise<ResponseWsDto> {
    
    let url: string = `${AppSetting.API}/api/v1/CreditNote/createCode`;
    let RespuestaWS : ResponseWsDto;

    RespuestaWS = await this.apiService.ExecuteGetService(url,{});

    return RespuestaWS;
  }

  async Confirm(Entity: CreditNoteRegisterDto): Promise<ResponseWsDto> {
    let url: string = `${AppSetting.API}/api/v1/CreditNote/confirm`;
    let RespuestaWS : ResponseWsDto;

    RespuestaWS = await this.apiService.ExecutePostService(url,Entity);

    return RespuestaWS;
  }

  async FindByDocumentCod(DocumentCod: string): Promise<ResponseWsDto> {
    
    let url: string = `${AppSetting.API}/api/v1/CreditNote/findByDocumentCod`;
    let RespuestaWS : ResponseWsDto;

    RespuestaWS = await this.apiService.ExecuteGetService(url,{DocumentCod : DocumentCod});

    return RespuestaWS;
  }

  async findDataPrint(CreditNoteCod: string): Promise<ResponseWsDto> {
    
    let url: string = `${AppSetting.API}/api/v1/CreditNote/findDataPrint`;
    let RespuestaWS : ResponseWsDto;

    RespuestaWS = await this.apiService.ExecuteGetService(url,{CreditNoteCod : CreditNoteCod});

    return RespuestaWS;
  }

}
