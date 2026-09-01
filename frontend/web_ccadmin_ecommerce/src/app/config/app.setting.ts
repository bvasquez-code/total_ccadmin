import { environment } from '../../environments/environment';

export class AppSetting {
  public static API: string = environment.settings.backend;
}
