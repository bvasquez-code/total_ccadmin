import { Injectable } from "@angular/core";
import { PucharseRequestRegisterDto } from "../model/dto/PucharseRequestRegisterDto";
import { PucharseDetailsDto } from "../model/dto/PucharseDetailsDto";
import { PucharseDetEntity } from "../model/entity/PucharseDetEntity";
import { StoreEntity } from "../../shared/model/entity/StoreEntity";
import { WarehouseEntity } from "../../shared/model/entity/WarehouseEntity";
import { ProductUnitHelper } from "../../shared/helper/ProductUnitHelper";

@Injectable({
    providedIn: 'root'
})
export class PucharsePrintService {

    private readonly PRINT_MODE_TICKET = "TICKET";
    private readonly PRINT_MODE_FULL_PAGE = "FULL_PAGE";

    printPucharseRequest(pucharseRequest: PucharseRequestRegisterDto): void {
        const head = pucharseRequest.Headboard;
        const rows = pucharseRequest.DetailList || [];

        this.print({
            title: "COMANDA INTERNA DE COMPRA",
            codeLabel: "Solicitud",
            code: head.PucharseReqCod,
            dealerCod: head.DealerCod,
            externalCod: head.ExternalCod,
            commenter: head.Commenter,
            storeCod: head.StoreCod,
            currencyCod: head.CurrencyCod,
            total: Number(head.NumTotalPrice || 0),
            rows: rows.map(item => ({
                productCod: item.ProductCod,
                productName: item.Product?.ProductName || "",
                numUnit: ProductUnitHelper.toVisibleQuantity(Number(item.NumUnit || 0), Number(item.ProductUnitFactor || 1)),
                productUnitName: item.ProductUnitName || "NIU",
                lotNumber: item.LotNumber,
                expirationDate: item.ExpirationDate,
                numUnitPrice: ProductUnitHelper.toVisibleUnitPrice(Number(item.NumUnitPrice || 0), Number(item.ProductUnitFactor || 1)),
                numTotalPrice: Number(item.NumTotalPrice || 0)
            }))
        }, this.resolvePrintMode(head));
    }

    printReception(pucharseDetails: PucharseDetailsDto, receivedRows: PucharseDetEntity[], store: StoreEntity, warehouseList: WarehouseEntity[]): void {
        const head = pucharseDetails.Headboard;
        const warehouseText = (warehouseList || [])
            .map(item => `${item.WarehouseCod} ${item.WarehouseName || ""}`.trim())
            .join(", ");

        this.print({
            title: "RECEPCION INTERNA DE COMPRA",
            codeLabel: "Compra",
            code: head.PucharseCod,
            secondaryLabel: "Solicitud",
            secondaryCode: head.PucharseReqCod,
            dealerCod: head.DealerCod,
            externalCod: head.ExternalCod,
            commenter: head.Commenter,
            storeCod: store?.StoreCod || head.StoreCod,
            storeName: store?.Name || "",
            warehouseText: warehouseText,
            currencyCod: head.CurrencyCod,
            total: Number(receivedRows.reduce((sum, item) => sum + Number(item.NumTotalPrice || 0), 0)),
            rows: (receivedRows || []).map(item => ({
                productCod: item.ProductCod,
                productName: item.Product?.ProductName || "",
                numUnit: ProductUnitHelper.toVisibleQuantity(Number(item.NumUnitDelivered || item.NumUnit || 0), Number(item.ProductUnitFactor || 1)),
                productUnitName: item.ProductUnitName || "NIU",
                lotNumber: item.LotNumber,
                expirationDate: item.ExpirationDate,
                numUnitPrice: ProductUnitHelper.toVisibleUnitPrice(Number(item.NumUnitPrice || 0), Number(item.ProductUnitFactor || 1)),
                numTotalPrice: Number(item.NumTotalPrice || 0)
            }))
        }, this.resolvePrintMode(head));
    }

    private print(data: any, printMode: string): void {
        if (printMode === this.PRINT_MODE_FULL_PAGE) {
            this.openAndPrint(this.renderFullPageHtml(data));
            return;
        }

        this.openAndPrint(this.renderTicketHtml(data));
    }

    private renderFullPageHtml(data: any): string {
        const rows = (data.rows || []).map((item: any) => `
            <tr>
                <td>${this.escape(item.productCod)}</td>
                <td>${this.escape(item.productName)}</td>
                <td class="right">${this.escape(`${this.formatNumber(item.numUnit, 0)} ${item.productUnitName || ""}`.trim())}</td>
                <td>${this.escape(this.lotText(item.lotNumber))}</td>
                <td>${this.escape(this.formatDate(item.expirationDate))}</td>
                <td class="right">${this.formatNumber(item.numUnitPrice, 2)}</td>
                <td class="right">${this.formatNumber(item.numTotalPrice, 2)}</td>
            </tr>
        `).join("");

        return `
            <!doctype html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>${this.escape(data.title)}</title>
                <style>
                    body { font-family: Arial, sans-serif; color: #111; margin: 18px; font-size: 12px; }
                    .title { text-align: center; font-size: 18px; font-weight: 700; margin-bottom: 4px; }
                    .subtitle { text-align: center; font-size: 11px; margin-bottom: 16px; color: #555; }
                    .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 4px 18px; margin-bottom: 14px; }
                    .label { font-weight: 700; }
                    table { width: 100%; border-collapse: collapse; }
                    th, td { border: 1px solid #bbb; padding: 5px; vertical-align: top; }
                    th { background: #f1f3f5; font-size: 11px; }
                    .right { text-align: right; }
                    .total { margin-top: 12px; text-align: right; font-weight: 700; font-size: 14px; }
                    @media print { body { margin: 10mm; } }
                </style>
            </head>
            <body>
                <div class="title">${this.escape(data.title)}</div>
                <div class="subtitle">Documento interno - no tributario</div>

                <div class="grid">
                    <div><span class="label">${this.escape(data.codeLabel)}:</span> ${this.escape(data.code)}</div>
                    <div><span class="label">Fecha:</span> ${this.formatDateTime(new Date())}</div>
                    ${data.secondaryCode ? `<div><span class="label">${this.escape(data.secondaryLabel)}:</span> ${this.escape(data.secondaryCode)}</div>` : ""}
                    <div><span class="label">Proveedor:</span> ${this.escape(data.dealerCod || "-")}</div>
                    <div><span class="label">Operacion ref.:</span> ${this.escape(data.externalCod || "-")}</div>
                    <div><span class="label">Tienda:</span> ${this.escape(`${data.storeCod || ""} ${data.storeName || ""}`.trim() || "-")}</div>
                    ${data.warehouseText ? `<div><span class="label">Almacen:</span> ${this.escape(data.warehouseText)}</div>` : ""}
                    <div><span class="label">Comentario:</span> ${this.escape(data.commenter || "-")}</div>
                </div>

                <table>
                    <thead>
                        <tr>
                            <th>Codigo</th>
                            <th>Producto</th>
                            <th>Cant.</th>
                            <th>Lote</th>
                            <th>Venc.</th>
                            <th>P. Unit.</th>
                            <th>Total</th>
                        </tr>
                    </thead>
                    <tbody>${rows}</tbody>
                </table>

                <div class="total">${this.escape(data.currencyCod || "")} ${this.formatNumber(data.total || 0, 2)}</div>
            </body>
            </html>
        `;
    }

    private renderTicketHtml(data: any): string {
        const rows = (data.rows || []).map((item: any) => `
            <div class="item">
                <div class="item-name">${this.escape(item.productCod)} - ${this.escape(item.productName || "")}</div>
                <div class="line lot-line">
                    <span>${this.escape(this.lotText(item.lotNumber))}</span>
                    <span>${this.escape(this.formatDate(item.expirationDate))}</span>
                </div>
                <div class="amount-line">
                    <span>${this.escape(`${this.formatNumber(item.numUnit, 0)} ${item.productUnitName || ""}`.trim())}</span>
                    <span>${this.formatNumber(item.numUnitPrice, 2)}</span>
                    <span>${this.formatNumber(item.numTotalPrice, 2)}</span>
                </div>
            </div>
        `).join("");

        return `
            <!doctype html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>${this.escape(data.title)}</title>
                <style>
                    @page { size: 80mm auto; margin: 0; }
                    * { box-sizing: border-box; }
                    body {
                        font-family: Arial, sans-serif;
                        color: #111;
                        margin: 0;
                        padding: 4mm 5mm;
                        width: 80mm;
                        font-size: 9px;
                        line-height: 1.25;
                    }
                    .center { text-align: center; }
                    .title { font-size: 11px; font-weight: 700; text-transform: uppercase; }
                    .sub { font-size: 8px; color: #444; margin-top: 2px; }
                    .sep { border-top: 1px dashed #111; margin: 6px 0; }
                    .row { display: flex; justify-content: space-between; gap: 6px; }
                    .label { font-weight: 700; }
                    .table-head { font-weight: 700; margin-bottom: 4px; }
                    .item { margin-bottom: 9px; }
                    .item-name { font-weight: 700; text-transform: uppercase; }
                    .line { display: flex; justify-content: space-between; gap: 6px; margin-top: 2px; }
                    .lot-line { color: #333; }
                    .amount-line {
                        display: grid;
                        grid-template-columns: 1fr 1.25fr 1.35fr;
                        gap: 5px;
                        margin-top: 2px;
                    }
                    .amount-line span { text-align: right; }
                    .amount-line span:first-child { text-align: left; }
                    .total { font-size: 12px; font-weight: 700; }
                    @media print {
                        body { padding: 4mm 5mm; }
                    }
                </style>
            </head>
            <body>
                <div class="center">
                    <div class="title">${this.escape(data.title)}</div>
                    <div class="sub">Documento interno - no tributario</div>
                </div>

                <div class="sep"></div>

                <div class="row"><span class="label">${this.escape(data.codeLabel)}</span><span>${this.escape(data.code)}</span></div>
                ${data.secondaryCode ? `<div class="row"><span class="label">${this.escape(data.secondaryLabel)}</span><span>${this.escape(data.secondaryCode)}</span></div>` : ""}
                <div class="row"><span class="label">Fecha</span><span>${this.formatDateTime(new Date())}</span></div>
                <div><span class="label">Proveedor:</span> ${this.escape(data.dealerCod || "-")}</div>
                <div><span class="label">Op. ref.:</span> ${this.escape(data.externalCod || "-")}</div>
                <div><span class="label">Tienda:</span> ${this.escape(`${data.storeCod || ""} ${data.storeName || ""}`.trim() || "-")}</div>
                ${data.warehouseText ? `<div><span class="label">Almacen:</span> ${this.escape(data.warehouseText)}</div>` : ""}
                ${data.commenter ? `<div><span class="label">Comentario:</span> ${this.escape(data.commenter)}</div>` : ""}

                <div class="sep"></div>

                <div class="table-head">
                    <div>PRODUCTO</div>
                    <div class="line"><span>LOTE</span><span>FEC. VENC.</span></div>
                    <div class="amount-line"><span>CANT.</span><span>PREC. UNI.</span><span>TOTAL</span></div>
                </div>
                <div class="sep"></div>

                ${rows}

                <div class="sep"></div>
                <div class="row total"><span>TOTAL</span><span>${this.escape(data.currencyCod || "")} ${this.formatNumber(data.total || 0, 2)}</span></div>
                <div class="sep"></div>
                <div class="center sub">Uso interno</div>
            </body>
            </html>
        `;
    }

    private openAndPrint(html: string): void {
        const win = window.open('', '_blank', 'width=900,height=700');
        if (!win) return;

        win.document.open();
        win.document.write(html);
        win.document.close();
        setTimeout(() => {
            win.focus();
            win.print();
        }, 300);
    }

    private lotText(value: string): string {
        return value && value.trim() ? value : "SN";
    }

    private resolvePrintMode(source: any): string {
        const printMode = String(
            source?.PrintMode
            || source?.PucharsePrintMode
            || source?.PurchasePrintMode
            || source?.InternalPrintMode
            || ""
        ).trim().toUpperCase();

        if (printMode === this.PRINT_MODE_FULL_PAGE) {
            return this.PRINT_MODE_FULL_PAGE;
        }

        return this.PRINT_MODE_TICKET;
    }

    private formatDate(value: any): string {
        if (!value) return "";
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) return "";
        return date.toLocaleDateString("es-PE");
    }

    private formatDateTime(value: Date): string {
        return `${value.toLocaleDateString("es-PE")} ${value.toLocaleTimeString("es-PE", { hour: "2-digit", minute: "2-digit" })}`;
    }

    private formatNumber(value: number, decimals: number): string {
        return Number(value || 0).toFixed(decimals);
    }

    private escape(value: any): string {
        return String(value ?? "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }
}
