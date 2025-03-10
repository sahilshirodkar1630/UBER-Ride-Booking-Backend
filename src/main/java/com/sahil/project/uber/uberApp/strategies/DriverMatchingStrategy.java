package com.sahil.project.uber.uberApp.strategies;

import com.sahil.project.uber.uberApp.dto.RideRequestDto;
import com.sahil.project.uber.uberApp.entities.Driver;
import com.sahil.project.uber.uberApp.entities.RideRequest;

import java.util.List;

public interface DriverMatchingStrategy {
     List<Driver> findMatchingDriver(RideRequest rideRequest);
}
