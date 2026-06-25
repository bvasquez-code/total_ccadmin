package com.local.app.pinpad.exception;

import com.local.app.pinpad.enums.PinpadErrorCode;
import com.local.app.pinpad.model.dto.ResponseWsDto;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PinpadExceptionHandler {

    @ExceptionHandler(PinpadPaymentException.class)
    public ResponseEntity<ResponseWsDto<Void>> handlePinpadPaymentException(PinpadPaymentException ex) {
        return ResponseEntity.status(ex.getHttpStatus())
                .body(ResponseWsDto.error(ex.getErrorCode().name(), ex.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<ResponseWsDto<Void>> handleValidation(Exception ex) {
        return ResponseEntity.badRequest()
                .body(ResponseWsDto.error(PinpadErrorCode.INVALID_REQUEST.name(), "Solicitud invalida"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseWsDto<Void>> handleUnexpected(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseWsDto.error(PinpadErrorCode.UNKNOWN_ERROR.name(), "Error interno del agente pinpad"));
    }
}
