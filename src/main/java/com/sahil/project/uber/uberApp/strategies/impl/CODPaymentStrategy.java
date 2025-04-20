package com.sahil.project.uber.uberApp.strategies.impl;

import com.sahil.project.uber.uberApp.entities.Driver;
import com.sahil.project.uber.uberApp.entities.Payment;
import com.sahil.project.uber.uberApp.entities.Wallet;
import com.sahil.project.uber.uberApp.entities.enums.PaymentStatus;
import com.sahil.project.uber.uberApp.entities.enums.TransactionMethod;
import com.sahil.project.uber.uberApp.repositories.PaymentRepository;
import com.sahil.project.uber.uberApp.services.PaymentService;
import com.sahil.project.uber.uberApp.services.WalletService;
import com.sahil.project.uber.uberApp.strategies.PaymentStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CODPaymentStrategy implements PaymentStrategy {

    private final WalletService walletService;
    private final PaymentRepository paymentRepository;
    @Override
    public void processPayment(Payment payment) {
        Driver driver = payment.getRide().getDriver();
        Wallet driverWallet = walletService.findByUser(driver.getUser());
        Double platformCommission = payment.getAmount()*PLATFORM_COMMISSION;

        walletService.deductMoneyFromWallet(driver.getUser(), platformCommission,null,
                payment.getRide(), TransactionMethod.RIDE);

        payment.setPaymentStatus(PaymentStatus.CONFIRMED);
        paymentRepository.save(payment);

    }
}
