import { Injectable } from '@angular/core';
import * as QRCode from 'qrcode';
import { SaleDetailDto } from '../model/dto/SaleDetailDto';
import { CurrencyEntity } from '../../shared/model/entity/CurrencyEntity';
import { SaleHeadEntity } from '../model/entity/SaleHeadEntity';
import { SaleDocumentEntity } from '../model/entity/SaleDocumentEntity';
import { SaleDetEntity } from '../model/entity/SaleDetEntity';
import { SalePaymentEntity } from '../../trxpayment/model/entity/SalePaymentEntity';
import { PersonEntity } from '../../person/model/entity/PersonEntity';
import { StoreInfoDto } from '../../shared/model/entity/StoreInfoDto';
import { PaymentMethodEntity } from '../../shared/model/entity/PaymentMethodEntity';
import { ResponseWsDto } from '../../shared/model/dto/ResponseWsDto';
import { CreditNoteHeadEntity } from '../model/entity/CreditNoteHeadEntity';
import { CreditNoteDocumentEntity } from '../model/entity/CreditNoteDocumentEntity';
import { CreditNoteDetDto } from '../model/dto/CreditNoteDetDto';
import { CreditNoteDetEntity } from '../model/entity/CreditNoteDetEntity';
import { CreditNoteDetailDto } from '../model/dto/CreditNoteDetailDto';
import { TransferRequestDetailDto } from '../../transfer/model/dto/TransferRequestDetailDto';
import { TransferDocumentEntity } from '../../transfer/model/entity/TransferDocumentEntity';
import { TransferDetEntity } from '../../transfer/model/entity/TransferDetEntity';
import { ProductUnitHelper } from '../../shared/helper/ProductUnitHelper';

@Injectable({ providedIn: 'root' })
export class TicketSunatService {

  /** ==== Ajustes rápidos (puedes tunear estos) ==== */
  private readonly PAPER_WIDTH_MM = 80;  // ancho del rollo.
  private readonly LEFT_OFFSET_MM = 2;    // empuje a la derecha (2–4mm recomendado)
  private readonly H_PADDING_MM = 5;    // padding horizontal interno (más chico = más ancho útil)
  private readonly BASE_FONT_PX = 9;   // sube a 13 si quieres “llenar” más

  private readonly DEFAULT_IGV_RATE = 0.18;

  /** Render principal: imprime con layout SUNAT 80mm */
  async printSalesInvoice(saleDetailPrint: ResponseWsDto) {
    // === Bloques del saleDetailPrint ===
    const saleBlock: SaleDetailDto = saleDetailPrint?.DataAdditional?.find((x: any) => x.Name === 'SaleDetail')?.Data;
    const currencies: CurrencyEntity[] = saleDetailPrint?.DataAdditional?.find((x: any) => x.Name === 'CurrencyList')?.Data || [];
    const storeBlock: StoreInfoDto = saleDetailPrint?.DataAdditional?.find((x: any) => x.Name === 'Store')?.Data;
    const paymentList: PaymentMethodEntity[] = saleDetailPrint?.DataAdditional?.find((x: any) => x.Name === 'PaymentMethodList')?.Data || [];

    const cab: SaleHeadEntity = saleBlock?.Headboard || {};
    const doc: SaleDocumentEntity = saleBlock?.SaleDocument || {};
    const items: SaleDetEntity[] = saleBlock?.DetailList || [];
    const payments: SalePaymentEntity[] = saleBlock?.DetailPayment || [];
    const person: PersonEntity = saleBlock?.Headboard?.Client?.Person || {};

    // === Moneda ===
    const currency = currencies.find((c: any) => c.CurrencyCod === cab?.CurrencyCod) || { CurrencySymbol: 'S/.' };

    // === Serie / Número / Tipo de CPE ===
    const [series, number] = String(doc?.DocumentCod || '').split('-');
    const docTypeCode = this.getSunatDocTypeCode(doc); // '01' factura / '03' boleta

    // === Compañía / tienda (encabezado) ===
    const company = storeBlock?.Company || {};
    const companyUbigeoTxt = storeBlock?.CompanyUbigeo || '';
    const store = storeBlock?.Store || {};
    const storeUbigeoTxt = storeBlock?.StoreUbigeo || '';

    const issuer = {
      ruc: company?.TaxId || '00000000000',
      razonSocial: company?.LegalName || (company?.TradeName || 'MI TIENDA S.A.C.'),
      domicilioFiscal: {
        direccion: company?.FiscalAddress || company?.Address || '',
        ubigeoTxt: companyUbigeoTxt || '',
        telefono: company?.Phone || ''
      },
      puntoEmision: {
        nombre: store?.Name || (store?.StoreCod ? `Tienda ${store.StoreCod}` : 'Punto de emisión'),
        direccion: store?.Address || '',
        ubigeoTxt: storeUbigeoTxt || ''
      },
      logoPath: company?.LogoPath || '' // si quieres mostrar logo más adelante
    };

    // === Cliente seguro y tipo doc SUNAT ===
    const customerFullName = this.safeFullName(person);
    const customerDocNumber = (person?.DocumentNum ?? '').toString().trim() || '00000000';
    const customerDocTypeSunat = this.mapCustomerDocTypeToSunat(person?.DocumentType);

    // === Totales y fechas ===
    const igvAmount = this.fmtNum(cab.NumTotalTax);
    const totalAmount = this.fmtNum(cab.NumTotalPrice);
    const issueDate = this.formatDateDDMMYYYY(String(cab.CreationDate));

    // === Hash opcional de tu PSE (si lo recibes) ===
    const HASH = '';

    // === QR SUNAT ===
    const qrText = [
      issuer.ruc,
      docTypeCode,
      series || '',
      (number || '').replace(/^0+/, ''),
      igvAmount,
      totalAmount,
      issueDate,
      customerDocTypeSunat,
      customerDocNumber,
      HASH
    ].join('|');

    const qrDataUrl = await QRCode.toDataURL(qrText, { margin: 0 });

    // === HTML y a imprimir ===
    const html = this.renderSalesHTML({
      issuer: issuer,
      document: {
        typeText: docTypeCode === '03' ? 'BOLETA DE VENTA ELECTRÓNICA' : 'FACTURA ELECTRÓNICA',
        typeCode: docTypeCode,
        series: series ?? '',
        number: number ?? '',
        date: this.formatDateDDMMYYYY(String(cab.CreationDate)),
        time: this.formatTimeHHMM(String(cab.CreationDate)),
        currencySymbol: currency.CurrencySymbol || 'S/.'
      },
      customer: {
        docType: customerDocTypeSunat,
        docNumber: customerDocNumber,
        name: customerFullName
      },
      items: items.map((it: any, index: number) => {
        const productCod = it?.ProductCod || '';
        const productName = it?.Product?.ProductName || it?.Product?.ProductDesc || '';
        const productUnitFactor = ProductUnitHelper.normalizeFactor(Number(it?.ProductUnitFactor || 1));

        return {
          item: it?.ItemNumber || index + 1,
          desc: this.buildProductCodeName(productCod, productName),
          cant: ProductUnitHelper.toVisibleQuantity(Number(it?.NumUnit || 0), productUnitFactor),
          unit: it?.ProductUnitName || 'NIU',
          pUnit: ProductUnitHelper.toVisibleUnitPrice(Number((it?.NumUnitPriceSale ?? it?.NumUnitPrice) || 0), productUnitFactor),
          total: it.NumTotalPrice,
          lot: this.printLotNumber(it?.LotNumber),
          expirationDate: this.formatDateOnlyDDMMYYYY(it?.ExpirationDate)
        };
      }),
      totals: {
        opGravada: this.fmtNum(cab.NumTotalPriceNoTax),
        igv: this.fmtNum(cab.NumTotalTax),
        total: this.fmtNum(cab.NumTotalPrice)
      },
      payments: payments.filter(e => e.TrxPayment?.TypeMovement === 'I').map((p: any) => ({
        medio: this.getPaymentDescription(p?.TrxPayment?.PaymentMethodCod, paymentList) || 'OTRO',
        monto: this.fmtNum(p.NumAmountPaid),
        ref: p?.TrxPayment?.TransactionId || ''
      })),
      qrDataUrl,
      qrText,
      tipDoc: "sale"
    });

    this.openAndPrint(html);
  }

  /** Render principal: imprime NOTA DE CRÉDITO 80mm (SUNAT 07) */
  /** Imprime NOTA DE CRÉDITO electrónica (SUNAT tipo 07) usando SOLO los campos del JSON dado */
  async printCreditNote(creditNotePrint: ResponseWsDto) {
    // === Bloques del payload EXACTOS al JSON ===
    const cnBlock: CreditNoteDetailDto = creditNotePrint?.DataAdditional?.find((x: any) => x.Name === 'CreditNoteDetail')?.Data;
    const currencies: CurrencyEntity[] = creditNotePrint?.DataAdditional?.find((x: any) => x.Name === 'CurrencyList')?.Data || [];
    const storeBlock: StoreInfoDto = creditNotePrint?.DataAdditional?.find((x: any) => x.Name === 'Store')?.Data;
    const paymentList: PaymentMethodEntity[] = creditNotePrint?.DataAdditional?.find((x: any) => x.Name === 'PaymentMethodList')?.Data || [];

    const head: CreditNoteHeadEntity = cnBlock?.Headboard || {};     // tiene: CreationDate, NumTotalPrice, TypeCreditNote, Commenter, SaleCod, CurrencyCod...
    const doc: CreditNoteDocumentEntity = cnBlock?.Document || {};     // tiene: DocumentCod, CounterfoilCod
    const rows: CreditNoteDetDto[] = cnBlock.DetailList || [];    // arreglo: [{ CreditNoteDet, Product }]
    const person: PersonEntity = cnBlock?.Client?.Person || {};
    const payments: SalePaymentEntity[] = cnBlock?.DetailPayment || [];
    const docRef: SaleDocumentEntity = cnBlock?.DocumentReference || {};

    // === Moneda ===
    const currency = currencies.find((c: any) => c.CurrencyCod === head?.CurrencyCod) || { CurrencySymbol: 'S/.' };

    // === Serie / Número / Tipo NC (07) ===
    const [series, number] = String(doc?.DocumentCod || '').split('-');
    const docTypeCode: '07' = '07'; // del JSON: CounterfoilCod = "07F001" => siempre 07

    // === Emisor (compañía/tienda) — SOLO con lo que viene en "Store" ===
    const company = storeBlock?.Company || {};
    const companyUbigeoTxt = storeBlock?.CompanyUbigeo || '';
    const store = storeBlock?.Store || {};
    const storeUbigeoTxt = storeBlock?.StoreUbigeo || '';

    const issuer = {
      ruc: company?.TaxId || '00000000000',
      razonSocial: company?.LegalName || (company?.TradeName || 'MI TIENDA S.A.C.'),
      domicilioFiscal: {
        direccion: company?.FiscalAddress || company?.Address || '',
        ubigeoTxt: companyUbigeoTxt || '',
        telefono: company?.Phone || ''
      },
      puntoEmision: {
        nombre: store?.Name || (store?.StoreCod ? `Tienda ${store.StoreCod}` : 'Punto de emisión'),
        direccion: store?.Address || '',
        ubigeoTxt: storeUbigeoTxt || ''
      },
      logoPath: company?.LogoPath || ''
    };

    // === Cliente: en tu JSON viene "Client": null. No inventamos estructura → usamos valores SUNAT por defecto. ===
    const customerFullName = this.safeFullName(person);
    const customerDocNumber = (person?.DocumentNum ?? '').toString().trim() || '00000000';
    const customerDocTypeSunat = this.mapCustomerDocTypeToSunat(person?.DocumentType);

    // === Totales/fechas — SOLO con campos presentes ===
    const totalAmount = Number(head?.NumTotalPrice || 0);
    const { base: opGravada, tax: igvAmount } = this.splitTaxValuesFromTotal(totalAmount, this.DEFAULT_IGV_RATE);
    const issueDate = this.formatDateDDMMYYYY(String(head?.CreationDate));
    const issueTime = this.formatTimeHHMM(String(head?.CreationDate));

    // === Hash PSE opcional (no está en el JSON) → vacío ===
    const HASH = '';

    // === QR SUNAT (orden estándar) ===
    const qrText = [
      issuer.ruc,
      docTypeCode,                             // '07'
      series || '',
      (number || '').replace(/^0+/, ''),
      this.fmtNum(igvAmount),
      this.fmtNum(totalAmount),
      issueDate,
      customerDocTypeSunat,
      customerDocNumber,
      HASH
    ].join('|');

    let qrDataUrl = '';
    try { qrDataUrl = await (QRCode as any).toDataURL(qrText, { margin: 0 }); } catch { /* noop */ }

    // === Items — SOLO con lo que viene: CreditNoteDet y Product ===
    const items = rows.map((r: any, index: number) => {
      const det = r?.CreditNoteDet || {};
      const prod = r?.Product || {};
      const productCod = det?.ProductCod || prod?.ProductCod || '';
      const productName = prod?.ProductName || prod?.ProductDesc || '';
      const productUnitFactor = ProductUnitHelper.normalizeFactor(Number(det?.ProductUnitFactor || 1));

      return {
        item: det?.ItemNumber || index + 1,
        desc: this.buildProductCodeName(productCod, productName),
        cant: ProductUnitHelper.toVisibleQuantity(Number(det?.NumUnit || 0), productUnitFactor),
        unit: det?.ProductUnitName || 'NIU',
        pUnit: ProductUnitHelper.toVisibleUnitPrice(Number(det?.NumUnitPriceSale || 0), productUnitFactor),
        total: det?.NumTotalPrice,
        lot: this.printLotNumber(det?.LotNumber),
        expirationDate: this.formatDateOnlyDDMMYYYY(det?.ExpirationDate)
      };
    });

    // === Etiquetas propias de NC (también vienen en tu JSON) ===
    const typeNC = (String(head?.TypeCreditNote || '').toUpperCase() === 'T') ? 'TOTAL' : 'PARCIAL';
    const comment = (head?.Commenter || '').toString();
    const saleRef = (docRef?.DocumentCod || '').toString();

    // === HTML base reutilizando tu render ===
    const html = this.renderCreditNoteHTML({
      issuer,
      document: {
        typeText: 'NOTA DE CRÉDITO ELECTRÓNICA',
        typeCode: docTypeCode,
        series: series ?? '',
        number: number ?? '',
        date: issueDate,
        time: issueTime,
        currencySymbol: currency?.CurrencySymbol || 'S/.'
      },
      customer: {
        docType: customerDocTypeSunat,
        docNumber: customerDocNumber,
        name: customerFullName
      },
      items,
      totals: {
        opGravada: this.fmtNum(opGravada),
        igv: this.fmtNum(igvAmount),
        total: this.fmtNum(totalAmount)
      },
      payments: payments.map((p: any) => ({
        medio: this.getPaymentDescription(p?.TrxPayment?.PaymentMethodCod, paymentList) || 'OTRO',
        monto: this.fmtNum(-1 * p.NumAmountPaid),
        ref: p?.TrxPayment?.TransactionId || ''
      })),
      qrDataUrl,
      qrText,
      tipDoc: "creditnote",
      creditNoteInfo: {
        typeNC,
        comment,
        saleRef
      }
    });

    this.openAndPrint(html);
  }

  async printTransferReferralGuide(transferPrint: ResponseWsDto) {
    const detail: TransferRequestDetailDto = transferPrint?.DataAdditional?.find((x: any) => x.Name === 'transferDetail')?.Data;
    const storeOrigin: StoreInfoDto = transferPrint?.DataAdditional?.find((x: any) => x.Name === 'storeOrigin')?.Data;
    const storeDest: StoreInfoDto = transferPrint?.DataAdditional?.find((x: any) => x.Name === 'storeDest')?.Data;

    const head = detail?.transferHead || detail?.transferHeadRequest || {};
    const doc: TransferDocumentEntity = detail?.transferDocumentList?.find((x: any) => x.DocumentRole === 'R') || detail?.transferDocumentList?.[0] || new TransferDocumentEntity();
    const items: TransferDetEntity[] = detail?.transferDetList?.length ? detail.transferDetList : (detail?.transferDetRequestList || []);

    const company = storeOrigin?.Company || {};
    const originStore = storeOrigin?.Store || {};
    const destStore = storeDest?.Store || {};
    const [series, number] = String(doc?.DocumentCod || '').split('-');
    const issueDate = this.formatDateDDMMYYYY(String(doc?.CreationDate || head?.DispatchDate || head?.CreationDate));
    const issueTime = this.formatTimeHHMM(String(doc?.CreationDate || head?.DispatchDate || head?.CreationDate));
    const transferDate = this.formatDateDDMMYYYY(String(head?.DispatchDate || doc?.CreationDate || head?.CreationDate));
    const docTypeCode: '09' = '09';

    const qrText = [
      company?.TaxId || '',
      docTypeCode,
      series || '',
      (number || '').replace(/^0+/, ''),
      issueDate,
      doc?.DriverDocType || '',
      doc?.DriverDocNumber || '',
      doc?.XmlHash || ''
    ].join('|');

    let qrDataUrl = '';
    try { qrDataUrl = await (QRCode as any).toDataURL(qrText, { margin: 0 }); } catch { /* noop */ }

    const html = this.renderTransferReferralGuideHTML({
      issuer: {
        ruc: company?.TaxId || '',
        legalName: company?.LegalName || company?.TradeName || '',
        tradeName: company?.TradeName || '',
        fiscalAddress: company?.FiscalAddress || company?.Address || '',
        companyUbigeo: storeOrigin?.CompanyUbigeo || '',
        phone: company?.Phone || '',
        email: company?.Email || ''
      },
      document: {
        typeText: 'GUIA DE REMISION REMITENTE ELECTRONICA',
        typeCode: docTypeCode,
        code: doc?.DocumentCod || '',
        series: series || '',
        number: number || '',
        issueDate,
        issueTime,
        transferDate,
        reason: this.getTransferReasonDescription(doc?.ReasonTransferCod, doc?.ReasonTransferDesc),
        transportMode: this.getTransportModeDescription(doc?.TransportModeCod),
        totalWeight: doc?.TotalWeightKg,
        packages: doc?.NumPackages
      },
      origin: {
        storeName: originStore?.Name || '',
        address: doc?.DepartureAddress || originStore?.Address || '',
        ubigeo: doc?.DepartureUbigeo || originStore?.UbigeoCod || '',
        ubigeoText: storeOrigin?.StoreUbigeo || ''
      },
      destination: {
        storeName: destStore?.Name || '',
        address: doc?.ArrivalAddress || destStore?.Address || '',
        ubigeo: doc?.ArrivalUbigeo || destStore?.UbigeoCod || '',
        ubigeoText: storeDest?.StoreUbigeo || ''
      },
      carrier: {
        ruc: doc?.CarrierRuc || '',
        name: doc?.CarrierName || '',
        vehiclePlate: doc?.VehiclePlate || '',
        driverDocType: doc?.DriverDocType || '',
        driverDocNumber: doc?.DriverDocNumber || '',
        driverLicense: doc?.DriverLicenseNumber || ''
      },
      items: items.map((it: any, index: number) => {
        const productCod = it?.ProductCod || '';
        const productName = it?.Product?.ProductName || it?.Product?.ProductDesc || '';
        const internalQuantity = Number((it?.NumUnitDispatch ?? it?.NumUnit) || 0);

        return {
          item: it?.ItemNumber || index + 1,
          description: this.buildProductCodeName(productCod, productName),
          quantity: ProductUnitHelper.toVisibleQuantity(internalQuantity, it?.ProductUnitFactor),
          unit: it?.ProductUnitName || 'NIU',
          lot: this.printLotNumber(it?.LotNumber),
          expirationDate: this.formatDateOnlyDDMMYYYY(it?.ExpirationDate)
        };
      }),
      observation: head?.Observation || '',
      qrDataUrl,
      qrText
    });

    this.openAndPrint(html);
  }



  /** ========= Helpers ========= */
  private fmtNum(n: number): string { return (Number(n || 0)).toFixed(2); }

  private formatQuantity(value: number): string {
    const number = Number(value || 0);
    if (Number.isInteger(number)) return number.toFixed(0);
    return number.toFixed(2).replace(/\.?0+$/, '');
  }

  private formatDateDDMMYYYY(iso: string): string {
    if (!iso) return '';
    const d = new Date(iso);
    const dd = String(d.getDate()).padStart(2, '0');
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const yyyy = d.getFullYear();
    return `${dd}/${mm}/${yyyy}`;
  }
  private formatDateOnlyDDMMYYYY(value: any): string {
    if (!value) return '';
    const text = String(value);
    const dateOnly = text.match(/^(\d{4})-(\d{2})-(\d{2})/);
    if (dateOnly) return `${dateOnly[3]}/${dateOnly[2]}/${dateOnly[1]}`;
    return this.formatDateDDMMYYYY(text);
  }
  private printLotNumber(value: any): string {
    const text = String(value ?? '').trim();
    return text && text.toUpperCase() !== 'SN' ? text : '';
  }
  private buildProductCodeName(productCod: string, productName: string): string {
    const code = (productCod || '').trim();
    const name = (productName || '').trim();
    if (code && name) return `${code} : ${name}`;
    return code || name;
  }
  private formatTimeHHMM(iso: string): string {
    if (!iso) return '';
    const d = new Date(iso);
    const hh = String(d.getHours()).padStart(2, '0');
    const mm = String(d.getMinutes()).padStart(2, '0');
    return `${hh}:${mm}`;
  }

  private getPaymentDescription(paymentMethodCod: string, paymentList: PaymentMethodEntity[]): string {
    const pm = paymentList?.find(p => p.PaymentMethodCod === paymentMethodCod);
    return pm ? pm.Description : paymentMethodCod;
  }

  private mapCustomerDocTypeToSunat(code: string | undefined): string {

    const mapDocumentSunat = {
      listDoc : [
        { code: '01', name: 'DNI' }, // DNI
        { code: '06', name: 'RUC' }, // RUC
        { code: '04', name: 'CE' },  // Carnet de extranjería
      ]
    };

    const nameDocument = mapDocumentSunat.listDoc.find((d) => d.code === code)?.name;
    return nameDocument || ''; // '0' para “sin documento” o tipo no reconocido
  }

  private getSunatDocTypeCode(doc: SaleDocumentEntity): '01' | '03' {
    const cc = String(doc?.CounterfoilCod || '');
    const pref2 = cc.substring(0, 2);
    if (pref2 === '01') return '01';
    if (pref2 === '03') return '03';
    return '03';
  }

  private safeFullName(person: PersonEntity): string {

    if(person?.PersonType === "01"){
      const Names = (person?.Names ?? '').toString().trim();
      const LastNames = (person?.LastNames ?? '').toString().trim();
      const full = `${Names} ${LastNames}`.trim();
      return full || '';
    }else if(person?.PersonType === "04"){
      return (person?.BusinessName ?? '').toString().trim() || '';
    }else{
      return '';
    }
  }

  private openAndPrint(html: string) {
    const win = window.open('', '_blank', 'width=520,height=800'); // un poco más ancho para vista previa
    if (!win) return;
    win.document.open();
    win.document.write(html);
    win.document.close();
    setTimeout(() => { try { win.focus(); win.print(); } finally { /* opcional: win.close(); */ } }, 300);
  }

  private escape(s: any): string {
    return String(s ?? '')
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  }

  /** Tipo doc SUNAT para Nota de Crédito (07).
   *  Si no hay CounterfoilCod, forzamos '07'. */
  private getSunatDocTypeCodeForCreditNote(doc: any): '07' {
    const cf = String(doc?.CounterfoilCod || '').trim();
    if (cf.startsWith('07')) return '07';
    return '07';
  }

  /** Cliente seguro desde la NC (muchas veces no llega completo).
   *  Devuelve tipo doc SUNAT, número y nombre. */
  private safeClientFromCreditNote(client: any): { docType: string; docNumber: string; name: string } {
    // Si tu ClientEntity tuviera Person, ajusta aquí.
    // En muchos payloads de NC, llega nulo: devolvemos “cliente varios”.
    let docType = '0';
    let docNumber = '00000000';
    let name = 'CLIENTE VARIOS';

    const person = client?.Person || client?.person || null;
    if (person) {
      name = this.safeFullName(person);
      docType = this.mapCustomerDocTypeToSunat(person?.DocumentType);
      docNumber = (person?.DocumentNum ?? '').toString().trim() || '00000000';
    }
    return { docType, docNumber, name };
  }

  /** Divide total en Operación Gravada e IGV con fallback de tasa.
   *  Si ya vienen NumTotalPriceNoTax / NumTotalTax, respétalos. */
  private splitTaxValues(total: number, baseFromDto?: number, taxFromDto?: number, rate = 0.18) {
    const totalN = Number(total || 0);
    if (baseFromDto != null && taxFromDto != null) {
      return { base: Number(baseFromDto), tax: Number(taxFromDto) };
    }
    const base = +(totalN / (1 + rate)).toFixed(2);
    const tax = +(totalN - base).toFixed(2);
    return { base, tax };
  }

  private splitTaxValuesFromTotal(total: number, rate = 0.18) {
    const totalN = Number(total || 0);
    const base = +(totalN / (1 + rate)).toFixed(2);
    const tax = +(totalN - base).toFixed(2);
    return { base, tax };
  }

  private getTransferReasonDescription(code: string, desc?: string): string {
    if (desc) return desc;
    const reasons: any = {
      '01': 'Venta',
      '02': 'Compra',
      '03': 'Consignacion',
      '04': 'Traslado entre establecimientos de la misma empresa',
      '13': 'Otros'
    };
    return reasons[code] || code || '';
  }

  private getTransportModeDescription(code: string): string {
    const modes: any = {
      '01': 'Transporte publico',
      '02': 'Transporte privado'
    };
    return modes[code] || code || '';
  }

  private renderTransferReferralGuideHTML(data: {
    issuer: any,
    document: any,
    origin: any,
    destination: any,
    carrier: any,
    items: any[],
    observation: string,
    qrDataUrl: string,
    qrText: string
  }): string {
    const lines = (arr: any[]) => arr.join('');
    const itemRows = lines(data.items.map((it: any) => {
      const meta = [
        it.lot ? `LOTE: ${this.escape(it.lot)}` : '',
        it.expirationDate ? `VENCIMIENTO: ${this.escape(it.expirationDate)}` : ''
      ].filter(Boolean).join(' | ');

      return `
      <div class="item">
        <div class="row">
          <div class="desc">${this.escape(it.item)}. ${this.escape(it.description)}</div>
          <div class="amt">${this.escape(it.quantity)} ${this.escape(it.unit)}</div>
        </div>
        ${meta ? `<div class="item-meta">${meta}</div>` : ''}
      </div>`;
    }));

    const LEFT = this.LEFT_OFFSET_MM;
    const PAD = this.H_PADDING_MM;
    const PRINTABLE_MAX_MM = 79;
    const calcWidth = Math.min(this.PAPER_WIDTH_MM - LEFT, PRINTABLE_MAX_MM);

    const css = `
      @media print {
        @page { size: ${this.PAPER_WIDTH_MM}mm auto; margin: 0; }
        body { margin: 0; }
      }
      body { font-family: monospace; margin: 0; background:#fff; }
      .wrap {
        width: ${this.PAPER_WIDTH_MM}mm;
        padding-left: ${LEFT}mm;
        box-sizing: border-box;
      }
      .ticket {
        width: ${calcWidth}mm;
        padding: ${PAD}mm ${PAD}mm ${PAD + 3}mm ${PAD}mm;
        box-sizing: border-box;
        font-size: ${this.BASE_FONT_PX}px;
        line-height: 1.25;
      }
      .center { text-align: center; }
      .bold { font-weight: bold; }
      .small { font-size: ${Math.max(this.BASE_FONT_PX - 1, 8)}px; }
      .item-meta { font-size: ${this.BASE_FONT_PX}px; line-height: 1.25; }
      .sep { border-top: 1px dashed #000; margin: 6px 0; }
      .row { display: flex; flex-direction: row; justify-content: space-between; gap: 6px; }
      .desc { width: 70%; word-wrap: break-word; }
      .amt { width: 28%; text-align: right; }
      .line { margin: 2px 0; word-wrap: break-word; }
      .label { font-weight: bold; }
      .table-head { font-weight: bold; margin-bottom: 4px; }
      .item { margin-bottom: 4px; }
      .qr { display:flex; justify-content:center; margin-top:6px; }
      .qr img { width: 28mm; height: 28mm; }
      h3,h4 { margin: 0; }
      .header { margin-bottom: 4px; }
      .footer { margin-top: 8px; text-align:center; }
    `;

    return `
<!doctype html>
<html>
<head>
<meta charset="utf-8"/>
<title>${this.escape(data.document.typeText)} ${this.escape(data.document.code)}</title>
<style>${css}</style>
</head>
<body>
  <div class="wrap">
    <div class="ticket">
      <div class="center header">
        <h3 class="bold">${this.escape(data.issuer.legalName)}</h3>
        <div class="bold">RUC: ${this.escape(data.issuer.ruc)}</div>

        <div class="sep"></div>
        <div class="small bold">DOMICILIO FISCAL</div>
        <div class="small">${this.escape(data.issuer.fiscalAddress)}</div>
        <div class="small">${this.escape(data.issuer.companyUbigeo)}</div>
        ${data.issuer.phone ? `<div class="small">Tel: ${this.escape(data.issuer.phone)}</div>` : ''}

        <div class="sep"></div>
        <h4 class="bold">${this.escape(data.document.typeText)}</h4>
        <div>${this.escape(data.document.code)}</div>
        <div class="small">Fec: ${this.escape(data.document.issueDate)} ${this.escape(data.document.issueTime)}</div>
      </div>

      <div class="sep"></div>
      <div class="small bold">DATOS DEL TRASLADO</div>
      <div class="small line"><span class="label">Fec. traslado:</span> ${this.escape(data.document.transferDate)}</div>
      <div class="small line"><span class="label">Motivo:</span> ${this.escape(data.document.reason)}</div>
      <div class="small line"><span class="label">Modalidad:</span> ${this.escape(data.document.transportMode)}</div>
      <div class="small line"><span class="label">Peso bruto:</span> ${this.escape(data.document.totalWeight || '')} KG</div>
      <div class="small line"><span class="label">Bultos:</span> ${this.escape(data.document.packages || '')}</div>

      <div class="sep"></div>
      <div class="small bold">PUNTO DE PARTIDA</div>
      <div class="small line">${this.escape(data.origin.storeName)}</div>
      <div class="small line">${this.escape(data.origin.address)}</div>
      <div class="small line">Ubigeo: ${this.escape(data.origin.ubigeo)} ${this.escape(data.origin.ubigeoText)}</div>

      <div class="sep"></div>
      <div class="small bold">PUNTO DE LLEGADA</div>
      <div class="small line">${this.escape(data.destination.storeName)}</div>
      <div class="small line">${this.escape(data.destination.address)}</div>
      <div class="small line">Ubigeo: ${this.escape(data.destination.ubigeo)} ${this.escape(data.destination.ubigeoText)}</div>

      <div class="sep"></div>
      <div class="small bold">TRANSPORTE</div>
      <div class="small line"><span class="label">Transportista:</span> ${this.escape(data.carrier.name)}</div>
      <div class="small line"><span class="label">RUC:</span> ${this.escape(data.carrier.ruc)}</div>
      <div class="small line"><span class="label">Placa:</span> ${this.escape(data.carrier.vehiclePlate)}</div>
      <div class="small line"><span class="label">Conductor:</span> ${this.escape(data.carrier.driverDocType)} - ${this.escape(data.carrier.driverDocNumber)}</div>
      <div class="small line"><span class="label">Licencia:</span> ${this.escape(data.carrier.driverLicense)}</div>

      <div class="sep"></div>
      <div class="small bold">BIENES TRANSPORTADOS</div>
      <div class="sep"></div>
      <div class="table-head">
        <div class="row"><span>PRODUCTO</span><span>CANT.</span></div>
      </div>
      <div class="sep"></div>
      ${itemRows}

      ${data.observation ? `<div class="sep"></div><div class="small bold">OBSERVACION</div><div class="small line">${this.escape(data.observation)}</div>` : ''}

      <div class="sep"></div>
      <div class="qr">
        ${data.qrDataUrl ? `<img src="${data.qrDataUrl}" alt="QR" />` : ''}
      </div>
      <div class="small center">* ${this.escape(data.document.typeText)} *</div>
      <div class="small center">Representacion impresa del documento electronico</div>
      <div class="small center">${this.escape(data.qrText)}</div>
      <div class="footer small">Consulte la validez del documento electronico en SUNAT.</div>
    </div>
  </div>
</body>
</html>`;
  }


  /** ========= HTML 80mm (más ancho útil) ========= */
  private renderSalesHTML(data: {
    issuer: any, document: any, customer: any, items: any[],
    totals: any, payments: any[], qrDataUrl: string, qrText: string, tipDoc: string
  }): string {
    return this.renderTransactionHTML({
      ...data,
      tipDoc: 'sale',
      detailTitle: 'DETALLE DE VENTA',
      footerText: 'Gracias por su compra!'
    });
  }

  private renderCreditNoteHTML(data: {
    issuer: any, document: any, customer: any, items: any[],
    totals: any, payments: any[], qrDataUrl: string, qrText: string, tipDoc: string,
    creditNoteInfo: any
  }): string {
    return this.renderTransactionHTML({
      ...data,
      tipDoc: 'creditnote',
      detailTitle: 'DETALLE DE NOTA DE CREDITO',
      footerText: 'Consulte la validez del comprobante en SUNAT.'
    });
  }

  private renderTransactionHTML(data: {
    issuer: any, document: any, customer: any, items: any[],
    totals: any, payments: any[], qrDataUrl: string, qrText: string, tipDoc: string,
    detailTitle: string, footerText: string, creditNoteInfo?: any
  }): string {

    const lines = (arr: any[]) => arr.join('');
    const itemRows = lines(data.items.map(i => {
      const meta = [
        i.lot ? `LOTE: ${this.escape(i.lot)}` : '',
        i.expirationDate ? `VENCIMIENTO: ${this.escape(i.expirationDate)}` : ''
      ].filter(Boolean).join(' | ');
      const desc = i.item ? `${this.escape(i.item)}. ${this.escape(i.desc)}` : this.escape(i.desc);
      const quantity = i.unit
        ? `${this.formatQuantity(i.cant)} ${this.escape(i.unit)}`
        : this.escape(i.cant);

      return `<div class="item">
       <div class="row">
         <div class="desc">${desc}</div>
         <div class="amt">${quantity}</div>
       </div>
       ${meta ? `<div class="item-meta">${meta}</div>` : ''}
       <div class="amount-line">
         <span></span>
         <span>${this.fmtNum(i.pUnit)}</span>
         <span>${this.fmtNum(i.total)}</span>
       </div>
      </div>`;
    }));
    const pagoRows = lines(data.payments.map(p =>
      `<div class="row"><div class="desc">${this.escape(p.medio)} <br>${this.escape(p.ref)}</div><div class="imp">${p.monto}</div></div>`
    ));

    // --- Cálculo de anchos efectivos (80mm real) ---
    const LEFT = this.LEFT_OFFSET_MM;
    const PAD = this.H_PADDING_MM;

    // Muchas térmicas 80mm imprimen ~72–80mm. Vamos al límite (79mm).
    const PRINTABLE_MAX_MM = 79;

    const calcWidth = Math.min(this.PAPER_WIDTH_MM - LEFT, PRINTABLE_MAX_MM);

    // --- CSS ---
    const css = `
      @media print {
        @page { size: ${this.PAPER_WIDTH_MM}mm auto; margin: 0; }
        body { margin: 0; }
      }
      body { font-family: monospace; margin: 0; background:#fff; }
      .wrap {
        width: ${this.PAPER_WIDTH_MM}mm;
        padding-left: ${LEFT}mm;             /* empuje a la derecha mínimo */
        box-sizing: border-box;
      }
      .ticket {
        width: ${calcWidth}mm;               /* usa casi todo el ancho imprimible */
        padding: ${PAD}mm ${PAD}mm ${PAD + 3}mm ${PAD}mm;
        box-sizing: border-box;
        font-size: ${this.BASE_FONT_PX}px;
        line-height: 1.25;
      }
      .center { text-align: center; }
      .bold { font-weight: bold; }
      .small { font-size: ${Math.max(this.BASE_FONT_PX - 1, 8)}px; }
      .item-meta { font-size: ${this.BASE_FONT_PX}px; line-height: 1.25; }
      .sep { border-top: 1px dashed #000; margin: 6px 0; }
      .row { display: flex; flex-direction: row; justify-content: space-between; }

      /* Columnas más anchas para descripción */
      .desc { width: 70%; word-wrap: break-word; }
      .amt  { width: 28%; text-align: right; }
      .imp  { width: 28%; text-align: right; }
      .spacer { width: 70%; }
      .table-head { font-weight: bold; margin-bottom: 4px; }
      .amount-line {
        display: grid;
        grid-template-columns: 1fr 1.25fr 1.35fr;
        gap: 5px;
        margin-top: 2px;
      }
      .amount-line span { text-align: right; }
      .amount-line span:first-child { text-align: left; }

      .kv, .subttl { display: flex; justify-content: space-between; }
      .qr { display:flex; justify-content:center; margin-top:6px; }
      h3,h4 { margin: 0; }
      .header { margin-bottom: 4px; }
      .footer { margin-top: 8px; text-align:center; }
      .logo { max-width: 44mm; max-height: 13mm; margin-bottom: 4px; }
      .item { margin-bottom: 2px; }
      `;


    // total pagado / vuelto
    const totalPagado = (data as any).payments?.reduce((a: number, p: any) => a + Number(p?.monto || 0), 0) || 0;
    const vuelto = (totalPagado - Number((data as any).totals?.total || 0));

    return `
<!doctype html>
<html>
<head>
<meta charset="utf-8"/>
<title>${this.escape(data.document.typeText)}</title>
<style>${css}</style>
</head>
<body>
  <div class="wrap">
    <div class="ticket">
      <div class="center header">
        <h3 class="bold">${this.escape(data.issuer.razonSocial)}</h3>
        <div class="bold">RUC: ${this.escape(data.issuer.ruc)}</div>

        <div class="sep"></div>
        <div class="small bold">DOMICILIO FISCAL</div>
        <div class="small">${this.escape(data.issuer.domicilioFiscal.direccion)}</div>
        <div class="small">${this.escape(data.issuer.domicilioFiscal.ubigeoTxt)}</div>
        ${data.issuer.domicilioFiscal.telefono ? `<div class="small">Tel: ${this.escape(data.issuer.domicilioFiscal.telefono)}</div>` : ''}

        <div class="sep"></div>
        <div class="small bold">PUNTO DE EMISIÓN</div>
        <div class="small">${this.escape(data.issuer.puntoEmision.nombre)}</div>
        <div class="small">${this.escape(data.issuer.puntoEmision.direccion)}</div>
        <div class="small">${this.escape(data.issuer.puntoEmision.ubigeoTxt)}</div>

        <div class="sep"></div>
        <h4 class="bold">${this.escape(data.document.typeText)}</h4>
        <div>${this.escape(data.document.series)}-${this.escape(data.document.number)}</div>
        <div class="small">Fec: ${this.escape(data.document.date)} ${this.escape(data.document.time)}</div>
      </div>

      <div class="small">
        ${data.customer.name ? `<div>Cliente: ${this.escape(data.customer.name)}</div>` : ''}
        ${data.customer.docType ? `<div>Doc: ${this.escape(data.customer.docType)} - ${this.escape(data.customer.docNumber)}</div>` : ''}
      </div>
      ${data.creditNoteInfo ? `<div class="small">
        <div class="bold">Tipo NC: ${this.escape(data.creditNoteInfo.typeNC)}</div>
        ${data.creditNoteInfo.comment ? `<div>Motivo: ${this.escape(data.creditNoteInfo.comment)}</div>` : ''}
        ${data.creditNoteInfo.saleRef ? `<div>Ref. Venta: ${this.escape(data.creditNoteInfo.saleRef)}</div>` : ''}
      </div>` : ''}

      <div class="sep"></div>
      <div class="row"><span><b>${this.escape(data.detailTitle)}</b></span></div>
      <div class="sep"></div>
      <div class="table-head">
        <div class="row"><span>PRODUCTO</span><span>CANT.</span></div>
        <div class="amount-line"><span></span><span>PREC. UNI.</span><span>TOTAL</span></div>
      </div>
      <div class="sep"></div>
      ${itemRows}

      <div class="sep"></div>
      <div class="subttl small">
        <span>Op. Gravada</span>
        <span>${this.escape(data.document.currencySymbol)} ${data.totals.opGravada}</span>
      </div>
      <div class="subttl small">
        <span>IGV (18%)</span>
        <span>${this.escape(data.document.currencySymbol)} ${data.totals.igv}</span>
      </div>
      <div class="subttl bold">
        <span>TOTAL</span>
        <span>${this.escape(data.document.currencySymbol)} ${data.totals.total}</span>
      </div>

      ${data.tipDoc === "sale" ? `<div class="sep"></div><div class="small bold">PAGOS</div>${pagoRows}` : ''}

      ${data.tipDoc === "sale" ? `<div class="subttl small">
        <span>Importe Total</span>
        <span>${this.escape(data.document.currencySymbol)} ${this.fmtNum(totalPagado)}</span>
      </div>` : ''}

      ${data.tipDoc === "sale" ? `<div class="subttl small">
        <span>Vuelto</span>
        <span>${this.escape(data.document.currencySymbol)} ${this.fmtNum(vuelto)}</span>
      </div>` : ''}

      <div class="sep"></div>
      <div class="qr">
        <img src="${data.qrDataUrl}" alt="QR" />
      </div>
      <div class="small center">* ${this.escape(data.document.typeText)} *</div>
      <div class="small center">Representación impresa del comprobante electrónico</div>
      <div class="footer small">${this.escape(data.footerText)}</div>
    </div>
  </div>
</body>
</html>`;
  }
}
