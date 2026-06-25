export class SaleConstants {

    public static PENDING : string = "P";
    public static CONFIRMED : string = "C";
    public static REJECTED : string = "R";
    public static FINALIZED : string = "F";

    private static STATUS_LIST = [
        { code : this.PENDING , description: "Pendiente" },
        { code : this.CONFIRMED , description: "Confirmado" },
        { code : this.REJECTED , description: "Rechazado" },
        { code : this.FINALIZED , description: "Finalizado" }
    ];

    public static getStatusDescription(status: string): string {
        const description = this.STATUS_LIST.find( e => e.code === status)?.description;

        if(description){
            return description;
        }else{
            return "desconocido";
        }
    }

}


export enum SaleStatus {
    PENDING = "P",
    CONFIRMED = "C",
    REJECTED = "R",
    FINALIZED = "F",
  }
  
  export const SaleStatusDescriptions: Record<SaleStatus, string> = {
    [SaleStatus.PENDING]: "Pendiente",
    [SaleStatus.CONFIRMED]: "Confirmado",
    [SaleStatus.REJECTED]: "Rechazado",
    [SaleStatus.FINALIZED]: "Finalizado",
  };