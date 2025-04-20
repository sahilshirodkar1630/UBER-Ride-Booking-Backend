package com.sahil.project.uber.uberApp.dto;

import com.sahil.project.uber.uberApp.entities.User;
import com.sahil.project.uber.uberApp.entities.WalletTransaction;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.Data;

import java.util.List;

@Data
public class WalletDto {
    private long id;
    private UserDto user;
    private List<WalletTransactionDto> transactions;
    private  Double balance;
}
