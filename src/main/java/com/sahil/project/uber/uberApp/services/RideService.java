package com.sahil.project.uber.uberApp.services;

import com.sahil.project.uber.uberApp.dto.RideRequestDto;
import com.sahil.project.uber.uberApp.entities.Driver;
import com.sahil.project.uber.uberApp.entities.Ride;
import com.sahil.project.uber.uberApp.entities.RideRequest;
import com.sahil.project.uber.uberApp.entities.enums.RideStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;


public interface RideService {
    Ride getRideById(Long rideId);
    void matchWithDrivers(RideRequestDto rideRequestDto);
    Ride createNewRide(RideRequest rideRequest, Driver driver);
    Ride updateRideStatus(Ride ride, RideStatus rideStatus);
    Page<Ride> getAllRidesOfRider(Long riderId, PageRequest pageRequest);
    Page<Ride> getAllRidesOfDriver(Long driverId, PageRequest pageRequest);
}
