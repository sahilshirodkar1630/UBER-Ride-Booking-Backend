package com.sahil.project.uber.uberApp.services.implementations;

import com.sahil.project.uber.uberApp.dto.WalletTransactionDto;
import com.sahil.project.uber.uberApp.entities.WalletTransaction;
import com.sahil.project.uber.uberApp.repositories.WalletTransactionRepository;
import com.sahil.project.uber.uberApp.services.WalletService;
import com.sahil.project.uber.uberApp.services.WalletTransactionService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WalletTransactionServiceImpl implements WalletTransactionService {

    private final WalletTransactionRepository walletTransactionRepository;

    @Override
    public void createNewWalletTransaction(WalletTransaction walletTransaction) {
        walletTransactionRepository.save(walletTransaction);
    }
}
