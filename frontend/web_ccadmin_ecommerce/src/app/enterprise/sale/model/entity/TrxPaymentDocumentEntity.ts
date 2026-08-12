import { AuditTableEntity } from '../../../shared/model/entity/AuditTableEntity';

export class TrxPaymentDocumentEntity extends AuditTableEntity {
  public TrxPaymentDocumentId: number | null = null;
  public TrxPaymentId: number | null = null;
  public DocumentType: string = '';
  public ContentEncoding: string = '';
  public Content: string = '';
  public FileName: string = '';
  public ContentType: string = '';
  public SizeBytes: number | null = null;
  public Sha256Hash: string = '';
  public SourceType: string = '';
  public PurgeAfterDate: Date | null = null;
}
