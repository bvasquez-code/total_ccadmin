export class RespuestaAdicionalEntity{

    public key : string = "";
    public objeto : any = null;

    AgregarRespuesta(key: string, objeto: any) 
    {
        this.key = key;
        this.objeto = objeto;
    }


}