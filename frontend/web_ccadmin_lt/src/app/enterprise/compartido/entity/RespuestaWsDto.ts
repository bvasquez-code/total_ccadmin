import { RespuestaAdicionalEntity } from "./RespuestaAdicionalEntity";

export class RespuestaWsDto{


    public cod_respuesta : string = "200";
    public mensaje : string = "Ok";
    public obj_respuesta : any = null;
    public id_error : string = "";
    public flg_error : boolean = false;
    public list_RespuestaAdicional : RespuestaAdicionalEntity[] = [];
    public list_MensajeAdicional : string[] = [];
    public fec_Info : Date = new Date();

    constructor()
    {

    } 

    cargarError(Error: any) {
        
        const ErrorResp  : RespuestaAdicionalEntity = new RespuestaAdicionalEntity();
        ErrorResp.AgregarRespuesta( "Error" , Error );

        this.flg_error = true;
        this.mensaje = Error.message;
        this.list_RespuestaAdicional.push( ErrorResp);

    }

}