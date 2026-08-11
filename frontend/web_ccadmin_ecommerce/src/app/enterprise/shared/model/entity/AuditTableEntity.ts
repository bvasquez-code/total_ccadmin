export class AuditTableEntity {
  public CreationUser: string = '';
  public CreationDate: Date = new Date();
  public ModifyUser: string = '';
  public ModifyDate: Date = new Date();
  public Status: string = 'A';
}
