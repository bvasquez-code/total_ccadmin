import { Injectable } from '@angular/core';
import { AppSetting } from 'src/app/config/app.setting';
import { ApiService } from '../../compartido/service/api.service';
import { ResponseWsDto } from '../../shared/model/dto/ResponseWsDto';
import { SearchDto } from '../../shared/model/dto/SearchDto';

@Injectable({
    providedIn: 'root'
})
export class ProductInfoStockService {

    constructor(private apiService: ApiService) { }

    async findAll(search: SearchDto): Promise<ResponseWsDto> {
        const url = `${AppSetting.API}/api/v1/productInfoStock/findAll`;
        return await this.apiService.ExecuteGetService(url, search);
    }
}
