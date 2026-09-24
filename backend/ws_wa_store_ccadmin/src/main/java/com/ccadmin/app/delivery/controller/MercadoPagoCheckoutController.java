package com.ccadmin.app.delivery.controller;

import com.ccadmin.app.delivery.model.dto.MercadoPagoRequestDto;
import com.ccadmin.app.delivery.model.dto.SaleDeliveryAccessRequestDto;
import com.ccadmin.app.delivery.service.MercadoPagoCheckoutCreateService;
import com.ccadmin.app.delivery.service.MercadoPagoCheckoutSearchService;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/delivery/sale/mercadoPago")
public class MercadoPagoCheckoutController {
    private final MercadoPagoCheckoutSearchService mercadoPagoCheckoutSearchService;
    private final MercadoPagoCheckoutCreateService mercadoPagoCheckoutCreateService;

    public MercadoPagoCheckoutController(MercadoPagoCheckoutSearchService mercadoPagoCheckoutSearchService,
                                        MercadoPagoCheckoutCreateService mercadoPagoCheckoutCreateService) {
        this.mercadoPagoCheckoutSearchService = mercadoPagoCheckoutSearchService;
        this.mercadoPagoCheckoutCreateService = mercadoPagoCheckoutCreateService;
    }

    @PostMapping("configuration")
    public ResponseEntity<ResponseWsDto> configuration(@RequestBody SaleDeliveryAccessRequestDto request) {
        try {
            return ResponseEntity.ok(new ResponseWsDto(mercadoPagoCheckoutSearchService.findCheckout(request.OrderToken)));
        } catch (Exception exception) {
            return failure(exception);
        }
    }

    @PostMapping("pay")
    public ResponseEntity<ResponseWsDto> pay(@RequestBody MercadoPagoRequestDto request) {
        try {
            return ResponseEntity.ok(new ResponseWsDto(mercadoPagoCheckoutCreateService.pay(request)));
        } catch (Exception exception) {
            return failure(exception);
        }
    }

    @PostMapping("status")
    public ResponseEntity<ResponseWsDto> status(@RequestBody SaleDeliveryAccessRequestDto request) {
        try {
            return ResponseEntity.ok(new ResponseWsDto(mercadoPagoCheckoutCreateService.refresh(request.OrderToken)));
        } catch (Exception exception) {
            return failure(exception);
        }
    }

    private ResponseEntity<ResponseWsDto> failure(Exception exception) {
        var response = new ResponseWsDto();
        response.ErrorStatus = true;
        response.Status = "400";
        response.Message = exception instanceof IllegalArgumentException || exception instanceof IllegalStateException
                ? exception.getMessage() : "No se pudo verificar el pago. Consulta su estado antes de reintentar.";
        return ResponseEntity.badRequest().body(response);
    }
}
