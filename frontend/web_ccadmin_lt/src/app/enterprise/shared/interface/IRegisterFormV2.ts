import { Router } from "@angular/router";

export interface IRegisterFormV2<T, ID, R> {

    GetParamUrl(router: Router):void;

    FindDataForm(Id : ID): Promise<void>;

    LoadingForm(Entity : R):void;

    Save(): Promise<void>;

}
