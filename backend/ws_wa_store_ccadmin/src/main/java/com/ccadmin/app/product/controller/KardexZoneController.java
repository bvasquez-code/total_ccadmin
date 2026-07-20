package com.ccadmin.app.product.controller;

import com.ccadmin.app.product.model.dto.KardexZoneSearchDto;
import com.ccadmin.app.product.service.KardexZoneSearchService;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/kardexZone")
public class KardexZoneController {

    @Autowired
    private KardexZoneSearchService kardexZoneSearchService;

    @GetMapping("findAll")
    public ResponseEntity<ResponseWsDto> findAll(
            @RequestParam(name = "Query", defaultValue = "") String Query,
            @RequestParam(name = "Page", defaultValue = "1") int Page,
            @RequestParam(name = "StoreCod") String StoreCod,
            @RequestParam(name = "ZoneStockMoved", defaultValue = "") String ZoneStockMoved,
            @RequestParam(name = "TypeOperation", defaultValue = "") String TypeOperation
    ) {
        try {
            KardexZoneSearchDto search = new KardexZoneSearchDto();
            search.Query = Query;
            search.Page = Page;
            search.StoreCod = StoreCod;
            search.ZoneStockMoved = ZoneStockMoved;
            search.TypeOperation = TypeOperation;

            return new ResponseEntity<>(
                    new ResponseWsDto(this.kardexZoneSearchService.findAll(search)),
                    HttpStatus.OK
            );
        } catch (Exception exception) {
            return new ResponseEntity<>(new ResponseWsDto(exception), HttpStatus.BAD_REQUEST);
        }
    }
}
