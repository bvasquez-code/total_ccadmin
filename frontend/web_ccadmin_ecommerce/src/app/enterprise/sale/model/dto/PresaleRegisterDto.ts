import { PresaleChannelEntity } from '../entity/PresaleChannelEntity';
import { PresaleDetEntity } from '../entity/PresaleDetEntity';
import { PresaleHeadEntity } from '../entity/PresaleHeadEntity';

export class PresaleRegisterDto {
  public Headboard: PresaleHeadEntity = new PresaleHeadEntity();
  public DetailList: PresaleDetEntity[] = [];
  public PresaleChannel: PresaleChannelEntity = new PresaleChannelEntity();
  public CreditNoteCod: string = '';

  public rebuild(): void {
    this.Headboard.NumPriceSubTotal = this.money(this.DetailList.reduce(
      (total, item) => total + item.NumUnitPrice * item.NumUnit,
      0
    ));
    this.Headboard.NumDiscount = this.money(this.DetailList.reduce(
      (total, item) => total + item.NumDiscount * item.NumUnit,
      0
    ));
    this.Headboard.NumTotalPrice = this.money(
      this.Headboard.NumPriceSubTotal - this.Headboard.NumDiscount
    );
  }

  private money(value: number): number {
    return Math.round((Number(value || 0) + Number.EPSILON) * 100) / 100;
  }
}
