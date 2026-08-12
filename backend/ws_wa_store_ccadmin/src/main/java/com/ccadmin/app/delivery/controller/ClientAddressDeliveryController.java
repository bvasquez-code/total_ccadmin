package com.ccadmin.app.delivery.controller;

import com.ccadmin.app.delivery.model.dto.DeliveryCoverageRequestDto;
import com.ccadmin.app.delivery.service.AddressGeocodingSearchService;
import com.ccadmin.app.delivery.service.ClientAddressDeliveryCreateService;
import com.ccadmin.app.delivery.service.ClientAddressDeliverySearchService;
import com.ccadmin.app.delivery.service.StoreDeliverySearchService;
import com.ccadmin.app.sale.model.entity.ClientAddressEntity;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/delivery/clientAddress")
public class ClientAddressDeliveryController {

    private final ClientAddressDeliveryCreateService clientAddressDeliveryCreateService;
    private final ClientAddressDeliverySearchService clientAddressDeliverySearchService;
    private final StoreDeliverySearchService storeDeliverySearchService;
    private final AddressGeocodingSearchService addressGeocodingSearchService;

    public ClientAddressDeliveryController(
            ClientAddressDeliveryCreateService clientAddressDeliveryCreateService,
            ClientAddressDeliverySearchService clientAddressDeliverySearchService,
            StoreDeliverySearchService storeDeliverySearchService,
            AddressGeocodingSearchService addressGeocodingSearchService
    ) {
        this.clientAddressDeliveryCreateService = clientAddressDeliveryCreateService;
        this.clientAddressDeliverySearchService = clientAddressDeliverySearchService;
        this.storeDeliverySearchService = storeDeliverySearchService;
        this.addressGeocodingSearchService = addressGeocodingSearchService;
    }

    @GetMapping("findAll")
    public ResponseEntity<ResponseWsDto> findAll() {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto(clientAddressDeliverySearchService.findAll()),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findCountries")
    public ResponseEntity<ResponseWsDto> findCountries() {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto(clientAddressDeliverySearchService.findCountries()),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("searchAddress")
    public ResponseEntity<ResponseWsDto> searchAddress(
            @RequestParam String Query,
            @RequestParam String CountryCod
    ) {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto(addressGeocodingSearchService.search(Query, CountryCod)),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findStates")
    public ResponseEntity<ResponseWsDto> findStates(@RequestParam String CountryCod) {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto(clientAddressDeliverySearchService.findStates(CountryCod)),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findCities")
    public ResponseEntity<ResponseWsDto> findCities(@RequestParam Long StateId) {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto(clientAddressDeliverySearchService.findCities(StateId)),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findPeruProvinceLocation")
    public ResponseEntity<ResponseWsDto> findPeruProvinceLocation(
            @RequestParam String ProvinceCod
    ) {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto(
                            clientAddressDeliverySearchService.findPeruProvinceLocation(ProvinceCod)
                    ),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findDepartments")
    public ResponseEntity<ResponseWsDto> findDepartments() {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto(clientAddressDeliverySearchService.findDepartments()),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findProvinces")
    public ResponseEntity<ResponseWsDto> findProvinces(@RequestParam String DepartmentCod) {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto(clientAddressDeliverySearchService.findProvinces(DepartmentCod)),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findDistricts")
    public ResponseEntity<ResponseWsDto> findDistricts(@RequestParam String ProvinceCod) {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto(clientAddressDeliverySearchService.findDistricts(ProvinceCod)),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("save")
    public ResponseEntity<ResponseWsDto> save(@RequestBody ClientAddressEntity request) {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto(clientAddressDeliveryCreateService.save(request)),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("validateCoverage")
    public ResponseEntity<ResponseWsDto> validateCoverage(
            @RequestBody DeliveryCoverageRequestDto request
    ) {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto(storeDeliverySearchService.validateCoverage(request)),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }
}
