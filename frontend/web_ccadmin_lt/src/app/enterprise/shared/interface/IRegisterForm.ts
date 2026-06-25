import { Router } from "@angular/router";

export interface IRegisterForm<T, U>
{

    GetParamUrl(router: Router):void;

    FindDataForm(Id : U): Promise<void>;

    LoadingForm(Entity : T):void;

    Save(): Promise<void>;

}