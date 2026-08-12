import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import Swal from 'sweetalert2';
import { CartService } from '../../../cart/service/cart.service';
import { ClientSessionService } from '../../../client/service/client-session.service';
import { ClientAddressService } from '../../../client/service/client-address.service';
import { DeliveryCoverageDto } from '../../../client/model/dto/DeliveryCoverageDto';
import { DeliveryCoverageRequestDto } from '../../../client/model/dto/DeliveryCoverageRequestDto';
import { ClientAddressEntity } from '../../../client/model/entity/ClientAddressEntity';
import { StoreContextDto } from '../../../store/model/dto/StoreContextDto';
import { StoreContextService } from '../../../store/service/store-context.service';
import { CheckoutDeliveryDto } from '../../model/dto/CheckoutDeliveryDto';
import { CheckoutRegisterDto } from '../../model/dto/CheckoutRegisterDto';
import { CheckoutConfirmationDto } from '../../model/dto/CheckoutConfirmationDto';
import { PresaleRegisterDto } from '../../model/dto/PresaleRegisterDto';
import { SaleDetailDto } from '../../model/dto/SaleDetailDto';
import { SalePaymentDeliveryRegisterDto } from '../../model/dto/SalePaymentDeliveryRegisterDto';
import { PaymentMethodEntity } from '../../model/entity/PaymentMethodEntity';
import { PresaleDetEntity } from '../../model/entity/PresaleDetEntity';
import { TrxPaymentEntity } from '../../model/entity/TrxPaymentEntity';
import { TrxPaymentDocumentEntity } from '../../model/entity/TrxPaymentDocumentEntity';
import { SaleBillingEntity } from '../../model/entity/SaleBillingEntity';
import { CheckoutService } from '../../service/checkout.service';
import { BillingIdentityService } from '../../service/billing-identity.service';
import { PersonEntity } from '../../../client/model/entity/ClientEntity';

interface DeliveryOption {
  Code: string;
  Name: string;
  Description: string;
  Icon: string;
}

@Component({
  selector: 'app-checkout',
  templateUrl: './checkout.component.html',
  styleUrls: ['./checkout.component.css']
})
export class CheckoutComponent implements OnInit {
  public StoreContext: StoreContextDto | null = null;
  public Delivery = new CheckoutDeliveryDto();
  public IsSubmitting: boolean = false;
  public PresaleCod: string = '';
  public SaleCod: string = '';
  public OrderToken: string = '';
  public SaleDetail: SaleDetailDto | null = null;
  public PaymentMethodList: PaymentMethodEntity[] = [];
  public SelectedPaymentMethodCod: string = '';
  public IsSavingPayment: boolean = false;
  public OrderLoadError: boolean = false;
  public AddressList: ClientAddressEntity[] = [];
  public Coverage: DeliveryCoverageDto | null = null;
  public IsLoadingAddresses: boolean = false;
  public IsValidatingCoverage: boolean = false;
  public IsAddressModalVisible: boolean = false;
  public PaymentProofDocument: TrxPaymentDocumentEntity | null = null;
  public PaymentProofPreview: string = '';
  public BillingDocumentType: string = '03';
  public BillingRuc: string = '';
  public BillingPerson: PersonEntity | null = null;
  public IsSearchingBilling: boolean = false;

  private pendingConfirmationRequest: PresaleRegisterDto | null = null;
  private readonly MaxPaymentProofSizeBytes: number = 10 * 1024 * 1024;

  public readonly DeliveryOptions: DeliveryOption[] = [
    { Code: 'DELIVERY', Name: 'Delivery cercano', Description: 'Envío directo desde la tienda.', Icon: 'fa-motorcycle' },
    { Code: 'STORE_PICKUP', Name: 'Recojo en tienda', Description: 'Te avisaremos cuando esté listo.', Icon: 'fa-store' },
    { Code: 'SCHEDULED_DELIVERY', Name: 'Entrega programada', Description: 'Coordinaremos fecha y operador.', Icon: 'fa-calendar-alt' }
  ];

  public constructor(
    public cartService: CartService,
    private storeContextService: StoreContextService,
    private clientSessionService: ClientSessionService,
    private clientAddressService: ClientAddressService,
    private checkoutService: CheckoutService,
    private billingIdentityService: BillingIdentityService,
    private activatedRoute: ActivatedRoute,
    private router: Router,
    private toastrService: ToastrService
  ) {
  }

  public ngOnInit(): void {
    void this.initialize();
  }

  public availableOptions(): DeliveryOption[] {
    return this.StoreContext ? this.DeliveryOptions : [];
  }

  public async selectDelivery(code: string): Promise<void> {
    this.Delivery.DeliveryTypeCod = code;
    this.Coverage = null;
    if (code === 'STORE_PICKUP') {
      this.Delivery.ClientAddressID = null;
      return;
    }

    if (this.AddressList.length === 0) {
      this.IsAddressModalVisible = true;
      return;
    }

    const selected = this.AddressList.find(
      address => address.ClientAddressID === this.Delivery.ClientAddressID
    ) || this.AddressList[0];
    await this.selectAddress(selected);
  }

  public openAddressModal(): void {
    this.IsAddressModalVisible = true;
  }

  public closeAddressModal(): void {
    this.IsAddressModalVisible = false;
  }

  public async addressSaved(address: ClientAddressEntity): Promise<void> {
    this.IsAddressModalVisible = false;
    this.synchronizeSavedAddress(address);
    await this.selectAddress(address);
  }

  public async selectAddress(address: ClientAddressEntity): Promise<void> {
    if (!address.ClientAddressID) return;
    this.Delivery.ClientAddressID = address.ClientAddressID;
    this.Delivery.Address = address.Address;
    this.Delivery.GeocodedAddress = address.GeocodedAddress || '';
    this.Delivery.Reference = address.Reference || '';
    this.Delivery.CountryCod = address.CountryCod || '';
    this.Delivery.CountryName = address.CountryName || '';
    this.Delivery.StateName = address.StateName || '';
    this.Delivery.CityName = address.CityName || '';
    this.Delivery.UbigeoCod = address.UbigeoCod || '';
    this.Delivery.Latitude = address.Latitude;
    this.Delivery.Longitude = address.Longitude;
    this.Delivery.Instructions = address.Instructions || '';
    if (this.Delivery.IsThirdParty !== 'S') {
      this.Delivery.Names = address.Names || this.Delivery.Names;
      this.Delivery.Phone = address.Phone || this.Delivery.Phone;
    }
    await this.validateSelectedAddressCoverage();
  }

  public isSelectedAddress(address: ClientAddressEntity): boolean {
    return !!address.ClientAddressID && address.ClientAddressID === this.Delivery.ClientAddressID;
  }

  public canConfirmOrder(): boolean {
    if (!this.isBillingValid()) return false;
    if (!this.Delivery.DeliveryTypeCod) return false;
    if (!this.requiresAddress()) return true;
    return !!this.Delivery.ClientAddressID
      && this.Coverage?.DeliveryTypeCod === this.Delivery.DeliveryTypeCod
      && this.Coverage?.IsAvailable === 'S'
      && !this.IsValidatingCoverage;
  }

  public requiresAddress(): boolean {
    return this.Delivery.DeliveryTypeCod === 'DELIVERY'
      || this.Delivery.DeliveryTypeCod === 'SCHEDULED_DELIVERY';
  }

  public requiresSchedule(): boolean {
    return this.Delivery.DeliveryTypeCod === 'SCHEDULED_DELIVERY';
  }

  public selectBillingDocument(documentType: string): void {
    this.BillingDocumentType = documentType;
    if (documentType === '03') {
      this.BillingRuc = '';
      this.BillingPerson = null;
    }
  }

  public async findBillingCompany(): Promise<void> {
    const ruc = (this.BillingRuc || '').replace(/\D/g, '');
    this.BillingRuc = ruc;
    this.BillingPerson = null;
    if (!/^\d{11}$/.test(ruc)) {
      this.toastrService.warning('Ingresa un RUC valido de 11 digitos.');
      return;
    }

    this.IsSearchingBilling = true;
    try {
      const person = await this.billingIdentityService.findCompanyByRuc(ruc);
      if (!person) {
        this.toastrService.warning('No fue posible obtener los datos del RUC indicado.');
        return;
      }
      this.BillingPerson = person;
    } finally {
      this.IsSearchingBilling = false;
    }
  }

  public isBillingValid(): boolean {
    if (this.BillingDocumentType === '03') return true;
    return this.BillingDocumentType === '01'
      && !!this.BillingPerson
      && /^\d{11}$/.test(this.BillingPerson.DocumentNum || '')
      && !!(this.BillingPerson.BusinessName || '').trim();
  }

  public subtotal(): number {
    return this.cartService.subtotal();
  }

  public currency(): string {
    return this.cartService.getCurrent().Items[0]?.CurrencyCod || 'PEN';
  }

  public async confirm(): Promise<void> {
    if (!this.validate()) return;

    this.IsSubmitting = true;
    try {
      if (!this.pendingConfirmationRequest) {
        const request = await this.createPresale();
        if (!request) return;
        this.pendingConfirmationRequest = request;
      }

      const response = await this.checkoutService.confirm(this.pendingConfirmationRequest);
      if (response.ErrorStatus) {
        this.toastrService.error(
          response.Message || 'La preventa fue registrada, pero no se pudo convertir en una venta pendiente.'
        );
        return;
      }

      const confirmation = response.Data as CheckoutConfirmationDto;
      const saleDetail = confirmation?.SaleDetail;
      if (!confirmation?.OrderToken
        || !saleDetail?.Headboard?.SaleCod
        || saleDetail.Headboard.SaleStatus !== 'P') {
        this.toastrService.error('El servidor no devolvió una venta pendiente válida.');
        return;
      }

      this.OrderToken = confirmation.OrderToken;
      this.SaleCod = saleDetail.Headboard.SaleCod;
      this.PresaleCod = saleDetail.Headboard.PresaleCod;
      this.SaleDetail = saleDetail;
      this.pendingConfirmationRequest = null;
      this.cartService.clear();
      await this.router.navigate(['/checkout'], {
        queryParams: { order: this.OrderToken },
        replaceUrl: true
      });
      await this.loadSaleData();
      this.toastrService.success('Pedido creado. Ahora puedes completar los pagos pendientes.');
    } finally {
      this.IsSubmitting = false;
    }
  }

  public async addPayment(): Promise<void> {
    const paymentMethod = this.selectedPaymentMethod();
    const amount = this.outstandingBalance();

    if (!this.SaleDetail || !paymentMethod) {
      this.toastrService.warning('Selecciona el medio de pago.');
      return;
    }
    if (this.hasRegisteredPayment() || amount <= 0) {
      await this.loadSaleData();
      return;
    }
    if (this.requiresPaymentProof() && !this.PaymentProofDocument) {
      this.toastrService.warning('Adjunta la imagen del comprobante antes de realizar el pago.');
      return;
    }

    const confirmation = await Swal.fire({
      title: `Confirmar pago de ${this.paymentAmountLabel()}`,
      text: `Método seleccionado: ${this.paymentMethodName(paymentMethod.PaymentMethodCod)}. Al continuar se procesará el pago del pedido.`,
      icon: 'question',
      showCancelButton: true,
      reverseButtons: true,
      focusCancel: true,
      confirmButtonColor: '#2878bd',
      cancelButtonColor: '#718694',
      confirmButtonText: 'Sí, realizar pago',
      cancelButtonText: 'Volver',
      heightAuto: false,
      customClass: {
        popup: 'store-payment-confirmation'
      }
    });
    if (!confirmation.isConfirmed) {
      return;
    }

    const request = new SalePaymentDeliveryRegisterDto();
    request.OrderToken = this.OrderToken;
    request.TrxPayment = this.buildPayment(paymentMethod, amount);
    request.DocumentList = this.PaymentProofDocument
      ? [this.PaymentProofDocument]
      : [];

    this.IsSavingPayment = true;
    try {
      const response = await this.checkoutService.addPayment(request);
      if (response.ErrorStatus) {
        this.toastrService.error(response.Message || 'No se pudo registrar el pago.');
        return;
      }

      await this.loadSaleData();
      this.clearPaymentProof();
      this.toastrService.success('Pago completado. El pedido continúa pendiente de confirmación por la tienda.');
    } finally {
      this.IsSavingPayment = false;
    }
  }

  public totalPaid(): number {
    return this.money((this.SaleDetail?.DetailPayment || [])
      .filter(payment => payment.Status === 'A')
      .reduce((total, payment) => total + Number(payment.NumAmountPaid || 0), 0));
  }

  public outstandingBalance(): number {
    const total = Number(this.SaleDetail?.Headboard?.NumTotalPrice || 0);
    return Math.max(0, this.money(total - this.totalPaid()));
  }

  public isPaymentComplete(): boolean {
    return this.SaleDetail?.Headboard?.IsPaid === 'S' || this.outstandingBalance() <= 0;
  }

  public hasRegisteredPayment(): boolean {
    return this.SaleDetail?.Headboard?.IsPaid === 'S'
      || (this.SaleDetail?.DetailPayment || []).length > 0;
  }

  public paymentAmountLabel(): string {
    const currencyCod = this.SaleDetail?.Headboard?.CurrencyCod || 'PEN';
    return `${currencyCod} ${this.outstandingBalance().toFixed(2)}`;
  }

  public selectedPaymentMethod(): PaymentMethodEntity | undefined {
    return this.PaymentMethodList.find(item => item.PaymentMethodCod === this.SelectedPaymentMethodCod);
  }

  public requiresPaymentProof(): boolean {
    return this.selectedPaymentMethod()?.IsPaymentProofRequired === 'S';
  }

  public onPaymentMethodChange(): void {
    if (!this.requiresPaymentProof()) {
      this.clearPaymentProof();
    }
  }

  public onPaymentProofSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    if (file.type !== 'image/jpeg' && file.type !== 'image/png') {
      this.toastrService.warning('El comprobante debe ser una imagen JPG o PNG.');
      input.value = '';
      return;
    }
    if (file.size <= 0 || file.size > this.MaxPaymentProofSizeBytes) {
      this.toastrService.warning('La imagen del comprobante no puede superar los 10 MB.');
      input.value = '';
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      const dataUrl = String(reader.result || '');
      const separator = dataUrl.indexOf(',');
      if (separator < 0) {
        this.toastrService.error('No se pudo leer la imagen seleccionada.');
        input.value = '';
        return;
      }

      const document = new TrxPaymentDocumentEntity();
      document.DocumentType = 'PAYMENT_PROOF';
      document.ContentEncoding = 'BASE64';
      document.Content = dataUrl.substring(separator + 1);
      document.FileName = file.name;
      document.ContentType = file.type;
      document.SizeBytes = file.size;
      document.SourceType = 'WEB';
      this.PaymentProofDocument = document;
      this.PaymentProofPreview = dataUrl;
      input.value = '';
    };
    reader.onerror = () => {
      this.toastrService.error('No se pudo leer la imagen seleccionada.');
      input.value = '';
    };
    reader.readAsDataURL(file);
  }

  public clearPaymentProof(): void {
    this.PaymentProofDocument = null;
    this.PaymentProofPreview = '';
  }

  public paymentMethodName(paymentMethodCod: string): string {
    const paymentMethod = this.PaymentMethodList.find(item => item.PaymentMethodCod === paymentMethodCod);
    return paymentMethod?.Description || paymentMethod?.Name || paymentMethodCod;
  }

  private async initialize(): Promise<void> {
    const orderToken = this.activatedRoute.snapshot.queryParamMap.get('order') || '';
    if (orderToken) {
      this.OrderToken = orderToken;
      await this.loadSaleData();
      return;
    }

    if (this.cartService.getCurrent().Items.length === 0) {
      void this.router.navigate(['/cart']);
      return;
    }

    this.StoreContext = this.storeContextService.getCurrent();
    if (!this.StoreContext || this.StoreContext.Store.StoreCod !== this.cartService.getCurrent().StoreCod) {
      this.toastrService.warning('Selecciona nuevamente la ubicación de la tienda antes de continuar.');
      void this.router.navigate(['/catalog']);
      return;
    }

    const session = this.clientSessionService.getCurrent();
    this.Delivery.Names = session?.Names || '';
    this.Delivery.Email = session?.Email || '';
    await this.loadAddresses();

    const options = this.availableOptions();
    const initialOption = options.find(option => option.Code === 'STORE_PICKUP') || options[0];
    if (initialOption) await this.selectDelivery(initialOption.Code);
  }

  private async loadAddresses(preferredAddressId: number | null = null): Promise<void> {
    this.IsLoadingAddresses = true;
    try {
      const response = await this.clientAddressService.findAll();
      if (response.ErrorStatus) {
        this.AddressList = [];
        this.toastrService.error(response.Message || 'No se pudieron consultar tus direcciones.');
        return;
      }
      this.AddressList = (response.Data || []).map(
        (item: ClientAddressEntity) => Object.assign(new ClientAddressEntity(), item)
      );

      if (!this.requiresAddress()) return;
      const selected = this.AddressList.find(
        address => address.ClientAddressID === preferredAddressId
      ) || this.AddressList.find(address => address.IsDefault === 'S') || this.AddressList[0];
      if (selected) await this.selectAddress(selected);
    } finally {
      this.IsLoadingAddresses = false;
    }
  }

  private synchronizeSavedAddress(savedAddress: ClientAddressEntity): void {
    const address = Object.assign(new ClientAddressEntity(), savedAddress);
    const remainingAddresses = this.AddressList
      .filter(item => item.ClientAddressID !== address.ClientAddressID)
      .map(item => {
        if (address.IsDefault === 'S') item.IsDefault = 'N';
        return item;
      });
    this.AddressList = address.IsDefault === 'S'
      ? [address, ...remainingAddresses]
      : [
          ...remainingAddresses.filter(item => item.IsDefault === 'S'),
          address,
          ...remainingAddresses.filter(item => item.IsDefault !== 'S')
        ];
  }

  private async validateSelectedAddressCoverage(): Promise<void> {
    if (!this.requiresAddress() || !this.Delivery.ClientAddressID) {
      this.Coverage = null;
      return;
    }

    const request = new DeliveryCoverageRequestDto();
    request.StoreCod = this.StoreContext?.Store.StoreCod || '';
    request.DeliveryTypeCod = this.Delivery.DeliveryTypeCod;
    request.Latitude = this.Delivery.Latitude;
    request.Longitude = this.Delivery.Longitude;

    this.IsValidatingCoverage = true;
    try {
      const response = await this.clientAddressService.validateCoverage(request);
      if (response.ErrorStatus || !response.Data) {
        this.Coverage = null;
        this.toastrService.error(response.Message || 'No se pudo validar la cobertura de esta dirección.');
        return;
      }
      this.Coverage = Object.assign(new DeliveryCoverageDto(), response.Data);
    } finally {
      this.IsValidatingCoverage = false;
    }
  }

  private async createPresale(): Promise<PresaleRegisterDto | null> {
    const storeCod = this.StoreContext?.Store.StoreCod || '';
    const codeResponse = await this.checkoutService.createCode(storeCod);
    if (codeResponse.ErrorStatus || !codeResponse.Data) {
      this.toastrService.error(codeResponse.Message || 'No se pudo generar el código del pedido.');
      return null;
    }

    const request = new CheckoutRegisterDto();
    const clientSession = this.clientSessionService.getCurrent();
    request.Headboard.PresaleCod = String(codeResponse.Data);
    request.Headboard.StoreCod = storeCod;
    request.Headboard.ClientCod = clientSession?.ClientCod || '';
    request.Headboard.CurrencyCod = this.currency();
    request.Headboard.IsPaid = 'N';
    request.Headboard.Client.ClientCod = clientSession?.ClientCod || '';
    request.Headboard.Client.PersonCod = clientSession?.ClientCod || '';
    request.Headboard.Client.Person.PersonCod = clientSession?.ClientCod || '';
    request.Headboard.Client.Person.Names = clientSession?.Names || '';
    request.Headboard.Client.Person.Email = clientSession?.Email || '';

    request.PresaleChannel.PresaleCod = request.Headboard.PresaleCod;
    request.PresaleChannel.ChannelCod = 'WEB';
    request.SaleBilling = this.buildSaleBilling();
    request.Delivery = Object.assign(new CheckoutDeliveryDto(), this.Delivery);
    request.DetailList = this.cartService.getCurrent().Items.map((item, index) => {
      const detail = new PresaleDetEntity();
      const factor = Math.max(1, Number(item.ProductUnitFactor || 1));
      const configuredPrice = Number(item.ProductInfo?.Config?.NumPrice);
      detail.PresaleCod = request.Headboard.PresaleCod;
      detail.ItemNumber = index + 1;
      detail.ProductCod = item.ProductCod;
      detail.Variant = '0000';
      detail.NumUnit = item.Quantity * factor;
      detail.NumUnitPrice = Number.isFinite(configuredPrice)
        ? configuredPrice
        : this.money(item.UnitPrice / factor);
      detail.NumDiscount = 0;
      detail.ProductUnitName = item.ProductUnitName || 'NIU';
      detail.ProductUnitFactor = factor;
      detail.IsDigital = item.IsDigital || 'N';
      detail.ProductInfo = item.ProductInfo;
      detail.recalculate();
      return detail;
    });
    request.rebuild();

    const response = await this.checkoutService.save(request);
    if (response.ErrorStatus) {
      this.toastrService.error(response.Message || 'No se pudo registrar el pedido.');
      return null;
    }

    const confirmation = new PresaleRegisterDto();
    confirmation.Headboard = response.Data?.Headboard || request.Headboard;
    confirmation.DetailList = response.Data?.DetailList || request.DetailList;
    confirmation.PresaleChannel = response.Data?.PresaleChannel || request.PresaleChannel;
    confirmation.SaleBilling = request.SaleBilling;
    confirmation.CreditNoteCod = '';
    return confirmation;
  }

  private async loadSaleData(): Promise<void> {
    this.OrderLoadError = false;
    const response = await this.checkoutService.findSaleData(this.OrderToken);
    if (response.ErrorStatus) {
      this.OrderLoadError = true;
      this.toastrService.error(response.Message || 'No se pudo consultar el pedido.');
      return;
    }

    this.SaleDetail = (response.DataAdditional.find(
      item => item.Name === 'SaleDetail'
    )?.Data as SaleDetailDto) || null;
    this.PaymentMethodList = (response.DataAdditional.find(
      item => item.Name === 'PaymentMethodList'
    )?.Data as PaymentMethodEntity[]) || [];
    this.PresaleCod = this.SaleDetail?.Headboard?.PresaleCod || '';
    this.SaleCod = this.SaleDetail?.Headboard?.SaleCod || '';

    if (!this.SelectedPaymentMethodCod
      || !this.PaymentMethodList.some(item => item.PaymentMethodCod === this.SelectedPaymentMethodCod)) {
      this.SelectedPaymentMethodCod = this.PaymentMethodList[0]?.PaymentMethodCod || '';
    }
  }

  private buildPayment(
    paymentMethod: PaymentMethodEntity,
    amount: number
  ): TrxPaymentEntity {
    const payment = new TrxPaymentEntity();
    payment.PaymentMethodCod = paymentMethod.PaymentMethodCod;
    payment.PaymentPlatform = this.isCash(paymentMethod)
      ? 'FISICO'
      : (this.isCard(paymentMethod) ? 'POS' : 'WEB');
    payment.PaymentStatus = 'OK';
    payment.CurrencyCod = this.SaleDetail?.Headboard?.CurrencyCod || 'PEN';
    payment.NumExchangevalue = Number(this.SaleDetail?.Headboard?.NumExchangevalue || 1);
    payment.AmountPaid = amount;
    payment.AmountReturned = 0;
    payment.TypeMovement = 'I';

    if (this.isCard(paymentMethod)) {
      payment.CardNumber = '4578************';
      payment.CardHolderName = 'Cliente web';
    }
    if (!this.isCash(paymentMethod)) {
      payment.TransactionId = this.generateTransactionId();
    }
    return payment;
  }

  private isCash(paymentMethod: PaymentMethodEntity): boolean {
    return paymentMethod.PaymentMethodType === '1001';
  }

  private isCard(paymentMethod: PaymentMethodEntity): boolean {
    return paymentMethod.PaymentMethodType === '1002' || paymentMethod.PaymentMethodType === '1003';
  }

  private generateTransactionId(): string {
    const now = new Date();
    return `WEB_${now.getTime()}_${Math.random().toString(36).slice(2, 8).toUpperCase()}`;
  }

  private validate(): boolean {
    if (!this.isBillingValid()) {
      this.toastrService.warning(
        this.BillingDocumentType === '01'
          ? 'Busca y confirma el RUC que se utilizara para la factura.'
          : 'Selecciona el comprobante que deseas recibir.'
      );
      return false;
    }
    if (!this.Delivery.DeliveryTypeCod) {
      this.toastrService.warning('No hay una modalidad de entrega disponible para esta ubicación.');
      return false;
    }
    if (!this.Delivery.Names.trim() || !this.Delivery.Phone.trim()) {
      this.toastrService.warning('Ingresa el nombre y teléfono de quien recibirá o recogerá el pedido.');
      return false;
    }
    if (this.Delivery.IsThirdParty === 'S' && !this.Delivery.DocumentNumber.trim()) {
      this.toastrService.warning('Ingresa el documento de la persona autorizada.');
      return false;
    }
    if (this.requiresAddress() && !this.Delivery.Address.trim()) {
      this.toastrService.warning('Selecciona una dirección de entrega.');
      return false;
    }
    if (this.requiresAddress() && !this.Delivery.ClientAddressID) {
      this.toastrService.warning('Selecciona una de tus direcciones o registra una nueva.');
      return false;
    }
    if (this.requiresAddress() && this.Coverage?.IsAvailable !== 'S') {
      this.toastrService.warning(
        this.Coverage?.Message || 'La dirección todavía no tiene una cobertura válida para esta modalidad.'
      );
      return false;
    }
    if (this.requiresSchedule() && (!this.Delivery.ScheduledFrom || !this.Delivery.ScheduledTo)) {
      this.toastrService.warning('Indica el rango de fecha y hora para la entrega programada.');
      return false;
    }
    if (this.Delivery.ScheduledFrom && this.Delivery.ScheduledTo
      && new Date(this.Delivery.ScheduledTo) < new Date(this.Delivery.ScheduledFrom)) {
      this.toastrService.warning('El final de la entrega programada no puede ser anterior al inicio.');
      return false;
    }
    return true;
  }

  private buildSaleBilling(): SaleBillingEntity {
    const saleBilling = new SaleBillingEntity();
    saleBilling.DocumentTypeRequest = this.BillingDocumentType;
    if (this.BillingDocumentType === '01' && this.BillingPerson) {
      saleBilling.PersonCod = this.BillingPerson.PersonCod || '';
      saleBilling.Person = Object.assign(new PersonEntity(), this.BillingPerson);
      saleBilling.DocumentType = this.BillingPerson.DocumentType;
      saleBilling.DocumentNum = this.BillingPerson.DocumentNum;
      saleBilling.LegalName = this.BillingPerson.BusinessName;
      saleBilling.CommercialName = this.BillingPerson.CommercialName;
      saleBilling.Address = this.BillingPerson.Address;
      saleBilling.UbigeoCod = this.BillingPerson.UbigeoCod;
    }
    return saleBilling;
  }

  private money(value: number): number {
    return Math.round((Number(value || 0) + Number.EPSILON) * 100) / 100;
  }

}
