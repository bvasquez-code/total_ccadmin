import { ResponseAdditionalDto } from './ResponseAdditionalDto';

export class ResponseWsDto {
  public Status: string = '';
  public Message: string = '';
  public Data: any = null;
  public ErrorStatus: boolean = false;
  public ErrorID: number = 0;
  public DataAdditional: ResponseAdditionalDto[] = [];

  public static fromError(error: any): ResponseWsDto {
    const response = Object.assign(new ResponseWsDto(), error?.error ?? {});
    response.ErrorStatus = true;
    response.Message = response.Message
      || error?.error?.mensaje
      || error?.message
      || 'No se pudo comunicar con el servidor.';
    return response;
  }
}
