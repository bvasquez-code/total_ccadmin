export class ProductUnitHelper {

    static normalizeFactor(productUnitFactor: number): number {
        return Number(productUnitFactor || 0) > 0 ? Number(productUnitFactor) : 1;
    }

    static toVisibleQuantity(internalQuantity: number, productUnitFactor: number): number {
        return Number(internalQuantity || 0) / this.normalizeFactor(productUnitFactor);
    }

    static toInternalQuantity(visibleQuantity: number, productUnitFactor: number): number {
        return Number(visibleQuantity || 0) * this.normalizeFactor(productUnitFactor);
    }

    static getVisibleStockAfterMovement(
        currentInternalStock: number,
        visibleMovementQuantity: number,
        productUnitFactor: number
    ): number {
        return this.toVisibleQuantity(currentInternalStock, productUnitFactor)
            - Number(visibleMovementQuantity || 0);
    }

    static toVisibleUnitPrice(internalUnitPrice: number, productUnitFactor: number): number {
        return Number(internalUnitPrice || 0) * this.normalizeFactor(productUnitFactor);
    }

    static toInternalUnitPrice(visibleUnitPrice: number, productUnitFactor: number): number {
        return Number(visibleUnitPrice || 0) / this.normalizeFactor(productUnitFactor);
    }

    static formatVisibleQuantity(internalQuantity: number, productUnitFactor: number, productUnitName: string): string {
        const unitName = productUnitName || 'NIU';
        return `${this.toVisibleQuantity(internalQuantity, productUnitFactor)} ${unitName}`;
    }

    static formatQuantity(quantity: number, maxDecimals: number = 2): string {
        return Number(Number(quantity || 0).toFixed(maxDecimals)).toString();
    }
}
