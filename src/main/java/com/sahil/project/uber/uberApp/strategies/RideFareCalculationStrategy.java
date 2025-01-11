package com.sahil.project.uber.uberApp.strategies;

import com.sahil.project.uber.uberApp.dto.RideRequestDto;

public interface RideFareCalculationStrategy {
    double calculateFare(RideRequestDto rideRequestDto);
}
