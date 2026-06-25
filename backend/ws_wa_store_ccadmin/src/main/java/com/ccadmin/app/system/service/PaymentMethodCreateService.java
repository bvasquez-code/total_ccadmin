package com.ccadmin.app.system.service;

import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.system.model.entity.PaymentMethodEntity;
import com.ccadmin.app.system.repository.PaymentMethodRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaymentMethodCreateService extends SessionService {

    @Autowired
    private PaymentMethodRepository paymentMethodRepository;

    public PaymentMethodEntity save(PaymentMethodEntity paymentMethod) {
        if (paymentMethod == null) {
            throw new IllegalArgumentException("Metodo de pago requerido");
        }
        paymentMethod.validate();
        paymentMethod.addSession(this.getUserCod(), !this.paymentMethodRepository.existsById(paymentMethod.PaymentMethodCod));
        return this.paymentMethodRepository.save(paymentMethod);
    }

    public List<PaymentMethodEntity> saveAll(List<PaymentMethodEntity> paymentMethodList) {
        if (paymentMethodList == null) {
            throw new IllegalArgumentException("Lista de metodos de pago requerida");
        }
        paymentMethodList.forEach(paymentMethod -> {
            paymentMethod.validate();
            paymentMethod.addSession(this.getUserCod(), !this.paymentMethodRepository.existsById(paymentMethod.PaymentMethodCod));
        });
        return this.paymentMethodRepository.saveAll(paymentMethodList);
    }

    public PaymentMethodEntity enable(PaymentMethodEntity request) {
        PaymentMethodEntity paymentMethod = this.paymentMethodRepository.findById(request.PaymentMethodCod)
                .orElseThrow(() -> new IllegalArgumentException("Metodo de pago no encontrado"));
        paymentMethod.active(this.getUserCod());
        return this.paymentMethodRepository.save(paymentMethod);
    }

    public PaymentMethodEntity disable(PaymentMethodEntity request) {
        PaymentMethodEntity paymentMethod = this.paymentMethodRepository.findById(request.PaymentMethodCod)
                .orElseThrow(() -> new IllegalArgumentException("Metodo de pago no encontrado"));
        paymentMethod.inactive(this.getUserCod());
        return this.paymentMethodRepository.save(paymentMethod);
    }
}
