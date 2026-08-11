package com.ccadmin.app.delivery.controller;

import com.ccadmin.app.delivery.model.dto.IpGeolocationDto;
import com.ccadmin.app.delivery.model.dto.StoreLocationRequestDto;
import com.ccadmin.app.delivery.service.IpGeolocationSearchService;
import com.ccadmin.app.delivery.service.StoreDeliverySearchService;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/delivery/store")
public class StoreDeliveryController {

    private final IpGeolocationSearchService ipGeolocationSearchService;
    private final StoreDeliverySearchService storeDeliverySearchService;

    public StoreDeliveryController(
            IpGeolocationSearchService ipGeolocationSearchService,
            StoreDeliverySearchService storeDeliverySearchService
    ) {
        this.ipGeolocationSearchService = ipGeolocationSearchService;
        this.storeDeliverySearchService = storeDeliverySearchService;
    }

    @GetMapping("resolveByIp")
    public ResponseEntity<ResponseWsDto> resolveByIp(HttpServletRequest request) {
        try {
            IpGeolocationDto location = ipGeolocationSearchService.findByRequest(request);
            StoreLocationRequestDto locationRequest = new StoreLocationRequestDto();
            locationRequest.Latitude = location.Latitude;
            locationRequest.Longitude = location.Longitude;
            locationRequest.Address = location.Address;
            locationRequest.IsManual = "N";
            return new ResponseEntity<>(
                    new ResponseWsDto(storeDeliverySearchService.resolveLocation(locationRequest)),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("resolveLocation")
    public ResponseEntity<ResponseWsDto> resolveLocation(@RequestBody StoreLocationRequestDto request) {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto(storeDeliverySearchService.resolveLocation(request)),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }
}
