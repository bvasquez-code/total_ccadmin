export type StockMovementKind = 'entry' | 'exit';
export type StockMovementMode = 'create' | 'view' | 'resolve';

export class StockMovementHead {
  StockEntryCod: string = '';
  StockExitCod: string = '';
  StoreCod: string = '';
  ProcessType: 'O' = 'O'; // O=Operacion original. Las resoluciones actualizan este mismo documento.
  MovementMode: 'D' | 'N' | null = 'D'; // D=Directo, N=No disponible
  ReasonCode: string | null = null;
  OriginStockEntryCod: string | null = null;
  OriginStockExitCod: string | null = null;
  ProcessStatus: 'P' | 'C' | 'R' | 'X' = 'P'; // P=Pendiente, C=Confirmado, R=Rechazado, X=Anulado
  NumTotalPrice: number = 0;
  Observation: string = '';
  CreationDate?: Date;
  ConfirmUser?: string;
  ConfirmDate?: Date;
  ResolutionUser?: string;
  ResolutionDate?: Date;
}

export class StockMovementDetail {
  Selected: boolean = false;
  VisibleQuantity: number = 0;
  StockEntryCod: string = '';
  StockExitCod: string = '';
  ItemNumber: number = 0;
  ProductCod: string = '';
  ProductName: string = '';
  Variant: string = '';
  WarehouseCod: string = '';
  LotNumber: string = '';
  ExpirationDate: string | null = null;
  ProductUnitName: string = 'NIU';
  ProductUnitFactor: number = 1;
  NumUnit: number = 0;
  NumUnitPrice: number = 0;
  NumTotalPrice: number = 0;
  NumUnitPending: number = 0;
  NumUnitResolvedIn: number = 0;
  NumUnitResolvedOut: number = 0;
  UnavailableReasonCode: string | null = null;
  ResolvedInReasonCode: string | null = null;
  ResolvedOutReasonCode: string | null = null;
  ResolvedOutType: 'B' | 'D' | null = null;
  ResolutionVersion: number = 0;
  OriginStockEntryCod: string | null = null;
  OriginStockExitCod: string | null = null;
  OriginItemNumber: number | null = null;
  ResolutionType: 'L' | 'B' | 'D' | 'M' | null = null; // L=Liberar, B=Baja, D=Destruir, M=Mantener
  ResolutionReasonCode: string | null = null;
  Observation: string = '';
  NextReviewDate: string | null = null;
}

export class StockMovementRegister {
  Head: StockMovementHead = new StockMovementHead();
  DetailList: StockMovementDetail[] = [];
}
