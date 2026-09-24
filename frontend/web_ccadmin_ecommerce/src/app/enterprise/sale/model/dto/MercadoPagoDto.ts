export interface MercadoPagoCardData {
  token: string;
  payment_method_id: string;
  issuer_id?: string;
  installments: number;
  payer: { email: string; identification?: { type: string; number: string } };
}

export interface MercadoPagoRequestDto {
  OrderToken: string;
  PaymentMethodCod: string;
  AttemptId: string;
  FormData: MercadoPagoCardData;
}

export interface MercadoPagoResultDto {
  AttemptId: string;
  PaymentId?: string;
  PaymentMethodCod?: string;
  ProviderStatus?: string;
  ProviderStatusDetail?: string;
  State: 'P' | 'C' | 'F';
  Message: string;
}

export interface MercadoPagoCheckoutDto {
  PublicKey: string;
  Locale: string;
  CurrencyCod: string;
  Amount: number;
  MaxInstallments: number;
  TestMode: boolean;
  Payment: MercadoPagoResultDto | null;
}

export interface MercadoPagoBrickController { unmount(): void | Promise<void>; }

export interface MercadoPagoSdk {
  bricks(): {
    create(type: 'cardPayment', container: string, settings: {
      initialization: { amount: number };
      customization: {
        paymentMethods: { maxInstallments: number; types: { included: string[] } };
        visual: { style: { theme: string } };
      };
      callbacks: {
        onReady: () => void;
        onError: (error: { type: string }) => void;
        onSubmit: (data: MercadoPagoCardData) => Promise<void>;
      };
    }): Promise<MercadoPagoBrickController>;
  };
}

export type MercadoPagoConstructor = new (key: string, options: { locale: string }) => MercadoPagoSdk;
