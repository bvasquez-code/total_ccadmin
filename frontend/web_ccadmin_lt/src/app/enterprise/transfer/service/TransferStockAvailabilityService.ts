import { Injectable } from '@angular/core';
import { ProductService } from '../../product/service/product.service';
import { ProductUnitHelper } from '../../shared/helper/ProductUnitHelper';
import { ResponseWsDto } from '../../shared/model/dto/ResponseWsDto';
import { TransferRequestDetEntity } from '../model/entity/TransferRequestDetEntity';

export interface TransferStockShortage {
    productCod: string;
    productName: string;
    requestedInternalQuantity: number;
    availableInternalQuantity: number;
    productUnitName: string;
    productUnitFactor: number;
}

@Injectable({
    providedIn: 'root'
})
export class TransferStockAvailabilityService {

    constructor(private productService: ProductService) { }

    async findShortages(
        detailList: TransferRequestDetEntity[],
        storeCod: string
    ): Promise<TransferStockShortage[]> {
        const requestedByProduct = new Map<string, TransferStockShortage>();

        detailList.forEach(detail => {
            const current = requestedByProduct.get(detail.ProductCod);
            if (current) {
                current.requestedInternalQuantity += Number(detail.NumUnit || 0);
                return;
            }

            requestedByProduct.set(detail.ProductCod, {
                productCod: detail.ProductCod,
                productName: detail.Product?.ProductName || detail.ProductCod,
                requestedInternalQuantity: Number(detail.NumUnit || 0),
                availableInternalQuantity: 0,
                productUnitName: detail.ProductUnitName || 'NIU',
                productUnitFactor: ProductUnitHelper.normalizeFactor(detail.ProductUnitFactor)
            });
        });

        const availability = await Promise.all(
            Array.from(requestedByProduct.values()).map(async item => {
                const currentStock = await this.findCurrentPhysicalStock(item.productCod, storeCod);
                if (currentStock === null) return null;
                item.availableInternalQuantity = currentStock;

                return item;
            })
        );

        return availability.filter((item): item is TransferStockShortage =>
            item !== null
            && item.requestedInternalQuantity > item.availableInternalQuantity
        );
    }

    async findCurrentPhysicalStock(productCod: string, storeCod: string): Promise<number | null> {
        const response: ResponseWsDto = await this.productService.findDetailById(productCod, storeCod);
        if (response.ErrorStatus) return null;

        const infoList = response.Data?.InfoList ?? [];
        const storeInfoList = infoList.filter((info: any) => info.StoreCod === storeCod);
        const applicableInfoList = storeInfoList.length > 0 ? storeInfoList : infoList;

        return applicableInfoList.reduce(
            (total: number, info: any) => total + Number(info.NumPhysicalStock || 0),
            0
        );
    }

    formatShortageSummary(shortages: TransferStockShortage[], maxItems: number = 3): string {
        const summary = shortages.slice(0, maxItems).map(item => {
            const requested = ProductUnitHelper.toVisibleQuantity(
                item.requestedInternalQuantity,
                item.productUnitFactor
            );
            const available = ProductUnitHelper.toVisibleQuantity(
                item.availableInternalQuantity,
                item.productUnitFactor
            );

            return `${item.productName}: solicitado ${ProductUnitHelper.formatQuantity(requested)} ${item.productUnitName}, disponible ${ProductUnitHelper.formatQuantity(available)} ${item.productUnitName}`;
        }).join('; ');
        const additionalCount = shortages.length - maxItems;

        return additionalCount > 0
            ? `${summary}; y ${additionalCount} producto(s) más`
            : summary;
    }
}
