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
    private readonly PAPER_WIDTH_MM = 80;
    private readonly LEFT_OFFSET_MM = 2;
    private readonly H_PADDING_MM = 5;
    private readonly BASE_FONT_PX = 9;

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
            rows: rows.map((item, index) => ({
                item: item.ItemNumber || index + 1,
                productCod: item.ProductCod,
                productName: item.Product?.ProductName || "",
                productDescription: this.buildProductCodeName(item.ProductCod, item.Product?.ProductName || ""),
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
            rows: (receivedRows || []).map((item, index) => ({
                item: item.ItemNumber || index + 1,
                productCod: item.ProductCod,
                productName: item.Product?.ProductName || "",
                productDescription: this.buildProductCodeName(item.ProductCod, item.Product?.ProductName || ""),
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
        const rows = (data.rows || []).map((item: any) => {
            const meta = this.productMetaText(item);

            return `
            <tr>
                <td>
                    <div>${this.escape(this.productDetailText(item))}</div>
                    ${meta ? `<div class="small">${meta}</div>` : ""}
                </td>
                <td class="right">${this.escape(this.quantityText(item))}</td>
                <td class="right">${this.formatNumber(item.numUnitPrice, 2)}</td>
                <td class="right">${this.formatNumber(item.numTotalPrice, 2)}</td>
            </tr>
        `;
        }).join("");

        return `
            <!doctype html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>${this.escape(data.title)}</title>
                <style>
                    body { font-family: monospace; color: #111; margin: 18px; font-size: ${this.BASE_FONT_PX}px; line-height: 1.25; }
                    .title { text-align: center; font-size: ${this.BASE_FONT_PX}px; font-weight: bold; margin-bottom: 4px; }
                    .subtitle { text-align: center; font-size: ${Math.max(this.BASE_FONT_PX - 1, 8)}px; margin-bottom: 16px; color: #555; }
                    .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 4px 18px; margin-bottom: 14px; }
                    .label { font-weight: bold; }
                    table { width: 100%; border-collapse: collapse; }
                    th, td { border: 1px solid #bbb; padding: 5px; vertical-align: top; }
                    th { background: #f1f3f5; font-size: ${this.BASE_FONT_PX}px; }
                    .right { text-align: right; }
                    .small { font-size: ${Math.max(this.BASE_FONT_PX - 1, 8)}px; color: #333; margin-top: 2px; }
                    .total { margin-top: 12px; text-align: right; font-weight: bold; font-size: ${this.BASE_FONT_PX}px; }
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
                            <th>Producto</th>
                            <th>Cant.</th>
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
        const rows = (data.rows || []).map((item: any) => {
            const meta = this.productMetaText(item);

            return `
            <div class="item">
                <div class="row">
                    <div class="desc">${this.escape(this.productDetailText(item))}</div>
                    <div class="amt">${this.escape(this.quantityText(item))}</div>
                </div>
                ${meta ? `<div class="small">${meta}</div>` : ""}
                <div class="amount-line">
                    <span></span>
                    <span>${this.formatNumber(item.numUnitPrice, 2)}</span>
                    <span>${this.formatNumber(item.numTotalPrice, 2)}</span>
                </div>
            </div>
        `;
        }).join("");
        const LEFT = this.LEFT_OFFSET_MM;
        const PAD = this.H_PADDING_MM;
        const PRINTABLE_MAX_MM = 79;
        const calcWidth = Math.min(this.PAPER_WIDTH_MM - LEFT, PRINTABLE_MAX_MM);
        const css = `
                    @media print {
                        @page { size: ${this.PAPER_WIDTH_MM}mm auto; margin: 0; }
                        body { margin: 0; }
                    }
                    body { font-family: monospace; margin: 0; background: #fff; }
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
                    .sep { border-top: 1px dashed #000; margin: 6px 0; }
                    .row { display: flex; flex-direction: row; justify-content: space-between; gap: 6px; }
                    .label { font-weight: bold; }
                    .table-head { font-weight: bold; margin-bottom: 4px; }
                    .item { margin-bottom: 4px; }
                    .desc { width: 70%; word-wrap: break-word; }
                    .amt { width: 28%; text-align: right; }
                    .line { margin: 2px 0; word-wrap: break-word; }
                    .amount-line {
                        display: grid;
                        grid-template-columns: 1fr 1.25fr 1.35fr;
                        gap: 5px;
                        margin-top: 2px;
                    }
                    .amount-line span { text-align: right; }
                    .amount-line span:first-child { text-align: left; }
                    .total { font-size: ${this.BASE_FONT_PX}px; font-weight: bold; }
                    h3,h4 { margin: 0; }
                    .header { margin-bottom: 4px; }
                `;

        return `
            <!doctype html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>${this.escape(data.title)}</title>
                <style>${css}</style>
            </head>
            <body>
                <div class="wrap">
                    <div class="ticket">
                        <div class="center header">
                            <h4 class="bold">${this.escape(data.title)}</h4>
                            <div class="small">Documento interno - no tributario</div>
                        </div>

                        <div class="sep"></div>

                        <div class="row"><span class="label">${this.escape(data.codeLabel)}</span><span>${this.escape(data.code)}</span></div>
                        ${data.secondaryCode ? `<div class="row"><span class="label">${this.escape(data.secondaryLabel)}</span><span>${this.escape(data.secondaryCode)}</span></div>` : ""}
                        <div class="row"><span class="label">Fecha</span><span>${this.formatDateTime(new Date())}</span></div>
                        <div class="line"><span class="label">Proveedor:</span> ${this.escape(data.dealerCod || "-")}</div>
                        <div class="line"><span class="label">Op. ref.:</span> ${this.escape(data.externalCod || "-")}</div>
                        <div class="line"><span class="label">Tienda:</span> ${this.escape(`${data.storeCod || ""} ${data.storeName || ""}`.trim() || "-")}</div>
                        ${data.warehouseText ? `<div class="line"><span class="label">Almacen:</span> ${this.escape(data.warehouseText)}</div>` : ""}
                        ${data.commenter ? `<div class="line"><span class="label">Comentario:</span> ${this.escape(data.commenter)}</div>` : ""}

                        <div class="sep"></div>
                        <div class="small bold">DETALLE DE COMPRA</div>
                        <div class="sep"></div>
                        <div class="table-head">
                            <div class="row"><span>PRODUCTO</span><span>CANT.</span></div>
                            <div class="amount-line"><span></span><span>PREC. UNI.</span><span>TOTAL</span></div>
                        </div>
                        <div class="sep"></div>

                        ${rows}

                        <div class="sep"></div>
                        <div class="row total"><span>TOTAL</span><span>${this.escape(data.currencyCod || "")} ${this.formatNumber(data.total || 0, 2)}</span></div>
                        <div class="sep"></div>
                        <div class="center small">Uso interno</div>
                    </div>
                </div>
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

    private productMetaText(item: any): string {
        return [
            item.lotNumber ? `LOTE: ${this.escape(item.lotNumber)}` : "",
            item.expirationDate ? `VENCIMIENTO: ${this.escape(this.formatDate(item.expirationDate))}` : ""
        ].filter(Boolean).join(" | ");
    }

    private quantityText(item: any): string {
        return `${this.formatQuantity(item.numUnit)} ${item.productUnitName || ""}`.trim();
    }

    private buildProductCodeName(productCod: string, productName: string): string {
        const code = (productCod || "").trim();
        const name = (productName || "").trim();
        if (code && name) return `${code} : ${name}`;
        return code || name;
    }

    private productDetailText(item: any): string {
        const description = item.productDescription || this.buildProductCodeName(item.productCod, item.productName);
        return `${item.item || ""}. ${description}`.trim();
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
        const text = String(value);
        const dateOnly = text.match(/^(\d{4})-(\d{2})-(\d{2})/);
        if (dateOnly) return `${dateOnly[3]}/${dateOnly[2]}/${dateOnly[1]}`;
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

    private formatQuantity(value: number): string {
        const number = Number(value || 0);
        if (Number.isInteger(number)) return number.toFixed(0);
        return number.toFixed(2).replace(/\.?0+$/, "");
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
