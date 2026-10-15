import { ToastrService } from 'ngx-toastr';
import { ProductInfoDto } from '../../product/model/dto/ProductInfoDto';
import { ProductInfoEntity } from '../../product/model/entity/ProductInfoEntity';
import { ProductVariantEntity } from '../../product/model/entity/ProductVariantEntity';
import { ShoppingCartService } from './shoppingcart.service';

describe('ShoppingCartService product drafts', () => {
  let shoppingCartService: ShoppingCartService;
  let product: ProductInfoDto;
  let toastrService: jasmine.SpyObj<ToastrService>;
  let saveSession: jasmine.Spy;

  beforeEach(() => {
    toastrService = jasmine.createSpyObj<ToastrService>('ToastrService', ['error']);
    shoppingCartService = new ShoppingCartService(toastrService);
    saveSession = spyOn(sessionStorage, 'setItem');
    product = new ProductInfoDto();
    product.Product.ProductCod = 'PRODUCT-001';
    product.Config.NumPrice = 10;
    product.Config.ProductUnitFactor = 2;
    product.Config.IsDiscontable = 'S';
    product.Config.DiscountType = 'MP';
    product.Config.NumDiscountMax = 20;
    product.VariantList = ['A', 'B'].map(Variant => Object.assign(new ProductVariantEntity(), { Variant }));
    product.InfoList = ['A', 'B'].map(Variant => Object.assign(new ProductInfoEntity(), {
      Variant, NumDigitalStock: 10
    }));
  });

  it('keeps new quantities and discounts out of the cart and session until confirmation', () => {
    const draft = shoppingCartService.createProductDraft(product.Product.ProductCod);
    draft.HandbookUnit(product, product.VariantList[0], 2);
    draft.setManualDiscount(product, product.VariantList[0], 10);
    expect(shoppingCartService.getCart().DetailList).toEqual([]);
    expect(saveSession).not.toHaveBeenCalled();

    shoppingCartService.confirmProductDraft(product, draft);
    expect(shoppingCartService.getTotalProductVisible(product.Product.ProductCod)).toBe(2);
    expect(shoppingCartService.getCart().Headboard.NumTotalPrice).toBe(36);
    expect(saveSession).toHaveBeenCalledTimes(1);
  });

  it('preserves existing quantities and discounts when a draft is discarded', () => {
    shoppingCartService.HandbookUnit(product, product.VariantList[0], 2);
    shoppingCartService.setManualDiscount(product, product.VariantList[0], 10);
    saveSession.calls.reset();
    const draft = shoppingCartService.createProductDraft(product.Product.ProductCod);
    draft.HandbookUnit(product, product.VariantList[0], 3);
    draft.setManualDiscount(product, product.VariantList[0], 20);
    const reopenedDraft = shoppingCartService.createProductDraft(product.Product.ProductCod);
    expect(reopenedDraft.getTotalProductVisible(product.Product.ProductCod)).toBe(2);
    expect(reopenedDraft.GetProductInCart(product.Product.ProductCod, 'A')?.NumDiscount).toBe(1);
    expect(shoppingCartService.getCart().Headboard.NumTotalPrice).toBe(36);
    expect(saveSession).not.toHaveBeenCalled();
  });

  it('rejects unavailable stock while editing and keeps the last valid quantity', () => {
    const draft = shoppingCartService.createProductDraft(product.Product.ProductCod);
    draft.HandbookUnit(product, product.VariantList[0], 5);
    expect(() => draft.addUnit(product, product.VariantList[0])).toThrowError('Imposible stock');
    expect(() => draft.HandbookUnit(product, product.VariantList[0], 6)).toThrowError('Imposible stock');
    expect(draft.getTotalProductVisible(product.Product.ProductCod)).toBe(5);
    expect(toastrService.error).toHaveBeenCalled();
    expect(saveSession).not.toHaveBeenCalled();
  });

  it('validates every variant before committing any change', () => {
    shoppingCartService.addUnit(product, product.VariantList[0]);
    const draft = shoppingCartService.createProductDraft(product.Product.ProductCod);
    draft.HandbookUnit(product, product.VariantList[0], 2);
    draft.HandbookUnit(product, product.VariantList[1], 3);
    product.InfoList[1].NumDigitalStock = 2;
    saveSession.calls.reset();
    expect(() => shoppingCartService.confirmProductDraft(product, draft)).toThrowError('Imposible stock');
    expect(shoppingCartService.getTotalProductVariantVisible(product.Product.ProductCod, 'A')).toBe(1);
    expect(shoppingCartService.getTotalProductVariantVisible(product.Product.ProductCod, 'B')).toBe(0);
    expect(saveSession).not.toHaveBeenCalled();
  });

  it('commits multiple variants without changing other products', () => {
    const otherProduct = new ProductInfoDto();
    otherProduct.Product.ProductCod = 'OTHER';
    otherProduct.Config.IsDigital = 'S';
    shoppingCartService.addUnit(otherProduct, product.VariantList[0]);
    const draft = shoppingCartService.createProductDraft(product.Product.ProductCod);
    draft.HandbookUnit(product, product.VariantList[0], 2);
    draft.HandbookUnit(product, product.VariantList[1], 3);
    shoppingCartService.confirmProductDraft(product, draft);
    expect(shoppingCartService.getTotalProductVariantVisible(product.Product.ProductCod, 'A')).toBe(2);
    expect(shoppingCartService.getTotalProductVariantVisible(product.Product.ProductCod, 'B')).toBe(3);
    expect(shoppingCartService.getTotalProduct('OTHER')).toBe(1);
  });

  it('removes a zero quantity only when confirmed', () => {
    shoppingCartService.addUnit(product, product.VariantList[0]);
    const draft = shoppingCartService.createProductDraft(product.Product.ProductCod);
    draft.HandbookUnit(product, product.VariantList[0], 0);
    expect(shoppingCartService.getTotalProductVisible(product.Product.ProductCod)).toBe(1);
    shoppingCartService.confirmProductDraft(product, draft);
    expect(shoppingCartService.getCart().DetailList).toEqual([]);
  });

  it('allows unlimited digital stock and fractional visible quantities', () => {
    product.Config.IsDigital = 'S';
    const draft = shoppingCartService.createProductDraft(product.Product.ProductCod);
    draft.HandbookUnit(product, product.VariantList[0], 20.5);
    shoppingCartService.confirmProductDraft(product, draft);
    expect(shoppingCartService.getTotalProductVisible(product.Product.ProductCod)).toBe(20.5);
  });

  it('rejects invalid quantities and avoids adding empty lines', () => {
    const draft = shoppingCartService.createProductDraft(product.Product.ProductCod);
    [NaN, Infinity, -1].forEach(quantity => {
      expect(() => draft.HandbookUnit(product, product.VariantList[0], quantity)).toThrowError();
    });
    draft.HandbookUnit(product, product.VariantList[0], 0);
    expect(draft.getCart().DetailList).toEqual([]);
    expect(saveSession).not.toHaveBeenCalled();
  });
});
