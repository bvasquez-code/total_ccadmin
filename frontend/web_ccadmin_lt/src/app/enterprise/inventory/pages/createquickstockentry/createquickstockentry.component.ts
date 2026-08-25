import {
  AfterViewInit,
  Component,
  ElementRef,
  OnInit,
  ViewChild
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { AlertService } from '../../../shared/service/AlertService';
import { ProductService } from '../../../product/service/product.service';
import { StockMovementService } from '../../service/stock-movement.service';

@Component({
  selector: 'app-createquickstockentry',
  templateUrl: './createquickstockentry.component.html',
  styleUrls: ['./createquickstockentry.component.css']
})
export class CreateQuickStockEntryComponent implements OnInit, AfterViewInit {
  @ViewChild('quantityInput') quantityInput?: ElementRef<HTMLInputElement>;

  productCod = '';
  quantity = 0;
  quantityText = '0';
  saving = false;

  private barcode = '';
  private inputBuffer = '';
  private quantityBeforeSequence = 0;
  private lastDigitAt = 0;
  private scannerSequence = true;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private toastr: ToastrService,
    private alertService: AlertService,
    private productService: ProductService,
    private stockMovementService: StockMovementService
  ) {}

  ngOnInit(): void {
    this.productCod = this.clean(
      this.route.snapshot.queryParamMap.get('productCod')
    );
    if (!this.productCod) {
      this.toastr.error('No se recibió el código del producto');
      void this.router.navigate(['/enterprise/product/pages/listProduct']);
      return;
    }
    void this.loadProductBarcode();
  }

  ngAfterViewInit(): void {
    this.focusQuantity();
  }

  onQuantityKeyDown(event: KeyboardEvent): void {
    if (/^[a-zA-Z0-9]$/.test(event.key)) {
      const now = Date.now();
      const gap = now - this.lastDigitAt;
      if (!this.inputBuffer || gap > 300) {
        this.inputBuffer = event.key;
        this.quantityBeforeSequence = this.quantity;
        this.scannerSequence = true;
      } else {
        if (gap > 90) this.scannerSequence = false;
        this.inputBuffer += event.key;
      }
      this.lastDigitAt = now;
      return;
    }

    if (event.key === 'Enter') {
      event.preventDefault();
      this.commitQuantityInput();
      return;
    }

    if (event.key === 'Backspace' || event.key === 'Delete') {
      this.resetInputSequence();
    }
  }

  decrease(): void {
    this.quantity = Math.max(0, this.manualQuantity() - 1);
    this.syncQuantityText();
  }

  increase(): void {
    this.quantity = this.manualQuantity() + 1;
    this.syncQuantityText();
  }

  async save(): Promise<void> {
    if (this.saving || !this.commitQuantityInput()) return;
    if (!Number.isInteger(this.quantity) || this.quantity <= 0) {
      this.toastr.warning('Ingrese una cantidad mayor a cero');
      return;
    }

    const confirmation = await this.alertService.waring(
      `Se agregarán ${this.quantity} unidad(es) al stock del producto ${this.productCod}.`,
      'Confirmar carga inicial de stock'
    );
    if (!confirmation.isConfirmed) {
      this.focusQuantity();
      return;
    }

    this.saving = true;
    try {
      const response = await this.stockMovementService.quickCreateAndConfirm(
        this.productCod, this.quantity
      );
      if (response.ErrorStatus) {
        this.toastr.error(response.Message);
        return;
      }
      this.toastr.success('Stock inicial registrado correctamente');
      await this.router.navigate(['/enterprise/product/pages/listProduct']);
    } catch (error) {
      this.toastr.error('No fue posible registrar el stock inicial');
    } finally {
      this.saving = false;
    }
  }

  private async loadProductBarcode(): Promise<void> {
    try {
      const response = await this.productService.FindDataForm(this.productCod);
      if (response.ErrorStatus) return;
      const productRegister = response.DataAdditional
        ?.find(item => item.Name === 'product')?.Data;
      this.barcode = this.clean(productRegister?.productBarcode?.BarCode);
    } catch (error) {
      this.barcode = '';
    }
  }

  private commitQuantityInput(): boolean {
    const scannedValue = this.inputBuffer.trim();
    const looksLikeScanner = this.scannerSequence
      && scannedValue.length >= 6;

    if (looksLikeScanner) {
      if (this.matchesProduct(scannedValue)) {
        this.quantity = this.quantityBeforeSequence + 1;
        this.syncQuantityText();
        return true;
      }
      this.quantity = this.quantityBeforeSequence;
      this.syncQuantityText();
      this.toastr.warning('La pistola leyó un código distinto al producto creado');
      return false;
    }

    const value = this.clean(this.quantityText);
    if (!/^\d+$/.test(value)) {
      this.syncQuantityText();
      this.toastr.warning('Ingrese una cantidad entera válida');
      return false;
    }
    this.quantity = Number(value);
    this.syncQuantityText();
    return Number.isSafeInteger(this.quantity);
  }

  private matchesProduct(value: string): boolean {
    const normalizedValue = value.toUpperCase();
    return [this.productCod, this.barcode]
      .map(code => this.clean(code).toUpperCase())
      .filter(Boolean)
      .includes(normalizedValue);
  }

  private manualQuantity(): number {
    const value = Number(this.quantityText);
    return Number.isSafeInteger(value) && value >= 0 ? value : this.quantity;
  }

  private syncQuantityText(): void {
    this.quantityText = String(this.quantity);
    this.resetInputSequence();
    this.focusQuantity();
  }

  private resetInputSequence(): void {
    this.inputBuffer = '';
    this.lastDigitAt = 0;
    this.scannerSequence = true;
  }

  private focusQuantity(): void {
    setTimeout(() => {
      this.quantityInput?.nativeElement.focus();
      this.quantityInput?.nativeElement.select();
    }, 0);
  }

  private clean(value: unknown): string {
    return value === null || value === undefined ? '' : String(value).trim();
  }
}
