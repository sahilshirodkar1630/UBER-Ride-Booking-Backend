package com.sahil.project.uber.uberApp.strategies.impl;

import com.sahil.project.uber.uberApp.entities.RideRequest;
import com.sahil.project.uber.uberApp.services.DistanceService;
import com.sahil.project.uber.uberApp.strategies.RideFareCalculationStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class RideFareSurgePricingFareCalculationStrategy implements RideFareCalculationStrategy {
    private final DistanceService distanceService;
    private static final double SURGE_FACTOR = 2;

    @Override
    public double calculateFare(RideRequest rideRequest) {
        double distance = distanceService
                .calculateDistance(rideRequest.getPickupLocation(), rideRequest.getDropLocation());

        return distance*RIDE_FARE_MULTIPLIER*SURGE_FACTOR;
    }
}
