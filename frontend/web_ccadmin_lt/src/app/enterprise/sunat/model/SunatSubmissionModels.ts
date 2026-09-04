export interface SunatSubmission {
  SunatSubmissionCod: string;
  StoreCod: string;
  StoreName: string;
  SourceModule: string;
  SourceDocumentCod: string;
  SourceDocumentType: string;
  SunatDocumentType: string;
  Series: string;
  Correlative: number;
  RequestType: string;
  SendStatus: string;
  SunatStatus: string | null;
  RemoteSunatDocumentCod: string | null;
  SunatTicket: string | null;
  AttemptCount: number;
  LastAttemptDate: string | null;
  LastSuccessDate: string | null;
  LastAttemptUser: string | null;
  LastResponseStatus: string | null;
  LastErrorReason: string | null;
  CreationUser: string;
  CreationDate: string;
  ModifyUser: string | null;
  ModifyDate: string;
}

export interface SunatSubmissionSearch {
  Query: string;
  StoreCod: string;
  RequestType: string;
  SendStatus: string;
  DateStart: string | null;
  DateEnd: string | null;
  Page: number;
}
