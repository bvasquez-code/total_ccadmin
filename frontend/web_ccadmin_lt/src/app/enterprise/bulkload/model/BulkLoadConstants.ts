export class BulkLoadConstants {
  static readonly TYPE_PRODUCT_PRICE = 'PRODUCT_PRICE';
  static readonly TYPE_STOCK_ENTRY = 'STOCK_ENTRY';
  static readonly TYPE_PRODUCT_CREATE = 'PRODUCT_CREATE';
  static readonly TYPE_BRAND_CREATE = 'BRAND_CREATE';
  static readonly TYPE_CATEGORY_CREATE = 'CATEGORY_CREATE';
  static readonly USE_LEGACY_GENERATED_TEMPLATES = false;
  static readonly TEMPLATE_ASSET_PATH = 'assets/document/excel/formats';

  static readonly DRAFT = 'D';
  static readonly VALIDATING = 'V';
  static readonly PENDING = 'P';
  static readonly QUEUED = 'Q';
  static readonly WORKING = 'W';
  static readonly FINALIZED = 'F';
  static readonly ERROR = 'E';
  static readonly CANCELLED = 'X';
  static readonly CONFIRMED = 'C';

  static typeDescription(type: string): string {
    const descriptions: Record<string, string> = {
      PRODUCT_PRICE: 'Precios',
      STOCK_ENTRY: 'Stock',
      PRODUCT_CREATE: 'Creación de productos',
      BRAND_CREATE: 'Creación de marcas',
      CATEGORY_CREATE: 'Creación de categorías'
    };
    return descriptions[type] ?? type;
  }

  static requiresDestinations(type: string): boolean {
    return type === this.TYPE_PRODUCT_PRICE || type === this.TYPE_STOCK_ENTRY;
  }

  static statusDescription(status: string, numErrors: number = 0): string {
    const descriptions: Record<string, string> = {
      D: 'Borrador',
      V: 'Validando',
      P: 'Pendiente de confirmación',
      Q: 'En cola',
      W: 'Procesando',
      F: numErrors > 0 ? 'Finalizado con errores' : 'Finalizado',
      E: 'Error',
      X: 'Anulado',
      C: 'Confirmado'
    };
    return descriptions[status] ?? 'Desconocido';
  }

  static statusClass(status: string): string {
    const classes: Record<string, string> = {
      D: 'badge-secondary',
      V: 'badge-info',
      P: 'badge-warning',
      Q: 'badge-primary',
      W: 'badge-primary',
      F: 'badge-success',
      E: 'badge-danger',
      X: 'badge-secondary',
      C: 'badge-success'
    };
    return classes[status] ?? 'badge-secondary';
  }

  static isCorrectableError(head: {
    ProcessStatus: string;
    NumProcessedDetails: number;
    QueueDate?: string;
    StartDate?: string;
  }): boolean {
    return head.ProcessStatus === this.ERROR
      && Number(head.NumProcessedDetails ?? 0) === 0
      && !head.QueueDate
      && !head.StartDate;
  }

  static isEditable(head: {
    ProcessStatus: string;
    NumProcessedDetails: number;
    QueueDate?: string;
    StartDate?: string;
  }): boolean {
    return head.ProcessStatus === this.PENDING || head.ProcessStatus === this.ERROR;
  }
}
