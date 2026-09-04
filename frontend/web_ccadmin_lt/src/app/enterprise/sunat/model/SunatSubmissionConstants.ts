export class SunatSubmissionConstants {
  static readonly PENDING = 'P';
  static readonly SENDING = 'W';
  static readonly SENT = 'S';
  static readonly ERROR = 'E';
  static readonly SENDING_RETRY_DELAY_MILLIS = 5 * 60 * 1000;

  static requestTypeDescription(value: string): string {
    const descriptions: Record<string, string> = {
      INVOICE: 'Factura',
      RECEIPT: 'Boleta',
      CREDIT_NOTE: 'Nota de crédito',
      DEBIT_NOTE: 'Nota de débito',
      DESPATCH_ADVICE: 'Guía de remisión'
    };
    return descriptions[value] ?? value;
  }

  static sunatStatusDescription(value: string | null): string {
    if (!value) return 'Sin respuesta';
    const descriptions: Record<string, string> = {
      PEN: 'Pendiente',
      GEN: 'Generado',
      FIR: 'Firmado',
      ZIP: 'Comprimido',
      ENV: 'Enviado',
      TCK: 'Pendiente de ticket',
      ACE: 'Aceptado',
      OBS: 'Aceptado con observaciones',
      REJ: 'Rechazado',
      ERR: 'Error',
      RET: 'Pendiente de reintento',
      ANU: 'Anulado'
    };
    return descriptions[value] ?? value;
  }
}
