package com.sahil.project.uber.uberApp.strategies;

import com.sahil.project.uber.uberApp.entities.Payment;

public interface PaymentStrategy {
    static final Double PLATFORM_COMMISSION = 0.3;
    void processPayment(Payment payment);


}




