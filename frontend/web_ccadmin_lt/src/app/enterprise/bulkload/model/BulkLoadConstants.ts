export class BulkLoadConstants {
  static readonly TYPE_PRODUCT_PRICE = 'PRODUCT_PRICE';
  static readonly TYPE_STOCK_ENTRY = 'STOCK_ENTRY';

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
    return type === this.TYPE_PRODUCT_PRICE
      ? 'Precios'
      : type === this.TYPE_STOCK_ENTRY ? 'Stock' : type;
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
}
