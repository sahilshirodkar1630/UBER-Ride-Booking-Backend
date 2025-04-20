package com.sahil.project.uber.uberApp.dto;

import com.sahil.project.uber.uberApp.entities.Ride;
import com.sahil.project.uber.uberApp.entities.enums.PaymentMethod;
import com.sahil.project.uber.uberApp.entities.enums.PaymentStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToOne;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

public class PaymentDto {
    private long id;
    private PaymentMethod paymentMethod;
    private Ride ride;
    private Double amount;
    private PaymentStatus paymentStatus;
    private LocalDateTime paymentTime;
}
