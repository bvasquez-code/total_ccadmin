import { Injectable } from "@angular/core";
import { AppSetting } from "src/app/config/app.setting";
import { ApiService } from "../../compartido/service/api.service";
import { ResponseWsDto } from "../../shared/model/dto/ResponseWsDto";
import { ProductTaxConfigRegisterDto } from "../model/dto/ProductTaxConfigRegisterDto";

@Injectable({
    providedIn: 'root'
})
export class ProductTaxConfigService {

    constructor(private apiService: ApiService) { }

    async findDataForm(ProductCod: string, StoreCod: string): Promise<ResponseWsDto> {
        const url = `${AppSetting.API}/api/v1/productTaxConfig/findDataForm`;
        return await this.apiService.ExecuteGetService(url, { ProductCod, StoreCod });
    }

    async saveAllByProductStore(request: ProductTaxConfigRegisterDto): Promise<ResponseWsDto> {
        const url = `${AppSetting.API}/api/v1/productTaxConfig/saveAllByProductStore`;
        return await this.apiService.ExecutePostService(url, request);
    }
}
