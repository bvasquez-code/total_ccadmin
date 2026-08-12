import { AuditTableEntity } from 'src/app/enterprise/shared/model/entity/AuditTableEntity';

export class TrxPaymentDocumentEntity extends AuditTableEntity {
    public TrxPaymentDocumentId: number = 0;
    public TrxPaymentId: number = 0;
    public DocumentType: string = '';
    public ContentEncoding: string = '';
    public Content: string = '';
    public FileName: string = '';
    public ContentType: string = '';
    public SizeBytes: number = 0;
    public Sha256Hash: string = '';
    public SourceType: string = '';
    public PurgeAfterDate: Date | null = null;
    public PreviewUrl: string = '';
}
