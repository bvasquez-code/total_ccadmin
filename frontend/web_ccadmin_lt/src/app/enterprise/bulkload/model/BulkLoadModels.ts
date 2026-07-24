export interface BulkLoadHead {
  BulkLoadCod: string;
  BulkLoadType: string;
  SchemaVersion: number;
  ProcessStatus: string;
  OriginalFileName: string;
  NumSourceRows: number;
  NumDestinations: number;
  NumTotalDetails: number;
  NumProcessedDetails: number;
  NumSuccessDetails: number;
  NumErrorDetails: number;
  NumWarningDetails: number;
  ProgressPercent: number;
  StatusMessage: string;
  CreationUser: string;
  CreationDate: string;
  ValidationDate?: string;
  QueueDate?: string;
  StartDate?: string;
  EndDate?: string;
}

export interface BulkLoadDestination {
  BulkLoadCod: string;
  StoreCod: string;
  ProcessStatus: string;
  NumTotalDetails: number;
  NumProcessedDetails: number;
  NumSuccessDetails: number;
  NumErrorDetails: number;
  StatusMessage: string;
}

export interface BulkLoadDetail {
  BulkLoadCod: string;
  ItemNumber: number;
  SourceRowNumber: number;
  StoreCod: string;
  BusinessKey: string;
  Payload: Record<string, unknown>;
  ProcessStatus: string;
  ErrorDetail?: BulkLoadError[];
  WarningDetail?: BulkLoadError[];
  ResultData?: Record<string, unknown>;
}

export interface BulkLoadError {
  Sheet: string;
  RowNumber: number;
  StoreCod?: string;
  Field: string;
  Value: string;
  ErrorCode: string;
  ErrorDetail: string;
  WarningDetail?: string;
}

export interface BulkLoadRegister {
  Head: BulkLoadHead;
  DestinationList: BulkLoadDestination[];
  ErrorList: BulkLoadError[];
}

export interface BulkLoadSourceRow {
  RowNumber: number;
  ProductCod: string;
  Value: string;
}

export interface BulkLoadStoreRow {
  RowNumber: number;
  StoreCod: string;
}

export interface BulkLoadParsedRequest {
  BulkLoadType: string;
  SchemaVersion: number;
  OriginalFileName: string;
  RowList: BulkLoadSourceRow[];
  StoreList: BulkLoadStoreRow[];
}

export interface PageResponse<T> {
  resultSearch: T[];
  TotalPages: number;
  TotalResult: number;
  StarResult: number;
  EndResult: number;
  Page: number;
}
