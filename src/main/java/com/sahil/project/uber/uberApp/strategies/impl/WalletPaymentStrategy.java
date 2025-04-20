package com.sahil.project.uber.uberApp.strategies.impl;

import com.sahil.project.uber.uberApp.entities.Driver;
import com.sahil.project.uber.uberApp.entities.Payment;
import com.sahil.project.uber.uberApp.entities.Rider;
import com.sahil.project.uber.uberApp.entities.Wallet;
import com.sahil.project.uber.uberApp.entities.enums.PaymentStatus;
import com.sahil.project.uber.uberApp.entities.enums.TransactionMethod;
import com.sahil.project.uber.uberApp.repositories.PaymentRepository;
import com.sahil.project.uber.uberApp.services.PaymentService;
import com.sahil.project.uber.uberApp.services.WalletService;
import com.sahil.project.uber.uberApp.strategies.PaymentStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WalletPaymentStrategy implements PaymentStrategy {

    private final WalletService walletService;
    private final PaymentRepository paymentRepository;
    @Override
    @Transactional
    public void processPayment(Payment payment) {

        Driver driver = payment.getRide().getDriver();
        Double platformCommission = payment.getAmount()*PLATFORM_COMMISSION;

        Rider rider = payment.getRide().getRider();
        Wallet riderWallet = walletService.findByUser(rider.getUser());

        walletService.deductMoneyFromWallet(rider.getUser(),
                payment.getAmount(), null,
                payment.getRide(), TransactionMethod.RIDE);

        walletService.addMoneyToWallet(driver.getUser(),
                payment.getAmount()-platformCommission, null,
                payment.getRide(), TransactionMethod.RIDE);

        payment.setPaymentStatus(PaymentStatus.CONFIRMED);
        paymentRepository.save(payment);

    }
}
