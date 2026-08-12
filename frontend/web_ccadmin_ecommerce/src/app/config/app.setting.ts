import { environment } from '../../environments/environment';

export class AppSetting {
  public static API: string = environment.settings.backend;
  public static API_SUNAT_IDENTITY: string = environment.settings.sunatIdentity;
}
