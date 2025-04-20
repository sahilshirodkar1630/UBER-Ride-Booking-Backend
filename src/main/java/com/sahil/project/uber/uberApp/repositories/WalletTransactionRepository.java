package com.sahil.project.uber.uberApp.repositories;

import com.sahil.project.uber.uberApp.entities.Payment;
import com.sahil.project.uber.uberApp.entities.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction,Long> {
}
