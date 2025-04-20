package com.sahil.project.uber.uberApp.services;

import com.sahil.project.uber.uberApp.entities.Payment;
import com.sahil.project.uber.uberApp.entities.Ride;
import com.sahil.project.uber.uberApp.entities.enums.PaymentStatus;
import org.springframework.stereotype.Service;


public interface PaymentService {
    void processPayment(Ride ride);
    Payment createNewPayment(Ride ride);
    Payment updatePaymentStatus(Payment payment, PaymentStatus paymentStatus);
}
