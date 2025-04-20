package com.sahil.project.uber.uberApp.services;

import com.sahil.project.uber.uberApp.dto.WalletTransactionDto;
import com.sahil.project.uber.uberApp.entities.WalletTransaction;

public interface WalletTransactionService {
    void createNewWalletTransaction(WalletTransaction walletTransaction);
}
