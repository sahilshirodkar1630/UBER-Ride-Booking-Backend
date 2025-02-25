package com.sahil.project.uber.uberApp.dto;

import com.sahil.project.uber.uberApp.entities.Rider;
import com.sahil.project.uber.uberApp.entities.enums.PaymentMethod;
import com.sahil.project.uber.uberApp.entities.enums.RideRequestStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RideRequestDto {
    private long id;
    private PointDto pickupLocation;
    private PointDto dropLocation;
    private LocalDateTime requestedTime;
    private Rider rider;
    private PaymentMethod paymentMethod;
    private RideRequestStatus rideRequestStatus;
    private Double fare;
}
