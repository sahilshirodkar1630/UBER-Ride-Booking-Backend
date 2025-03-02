package com.sahil.project.uber.uberApp.services.implementations;

import com.sahil.project.uber.uberApp.dto.RideRequestDto;
import com.sahil.project.uber.uberApp.entities.Driver;
import com.sahil.project.uber.uberApp.entities.Ride;
import com.sahil.project.uber.uberApp.entities.RideRequest;
import com.sahil.project.uber.uberApp.entities.enums.RideRequestStatus;
import com.sahil.project.uber.uberApp.entities.enums.RideStatus;
import com.sahil.project.uber.uberApp.exceptions.ResourceNotFoundException;
import com.sahil.project.uber.uberApp.repositories.RideRepository;
import com.sahil.project.uber.uberApp.services.RideRequestService;
import com.sahil.project.uber.uberApp.services.RideService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@Service
@RequiredArgsConstructor
public class RideServiceImpl implements RideService {

    private final RideRepository rideRepository;
    private final RideRequestService rideRequestService;
    private final ModelMapper modelMapper;

    @Override
    public Ride getRideById(Long rideId) {
        return rideRepository.findById(rideId).orElseThrow(() -> new ResourceNotFoundException("Ride not found for rideId: "+rideId));
    }

    @Override
    public void matchWithDrivers(RideRequestDto rideRequestDto) {

    }

    @Override
    @Transactional
    public Ride createNewRide(RideRequest rideRequest, Driver driver) {
        rideRequest.setRideRequestStatus(RideRequestStatus.CONFIRMED);
        Ride ride = modelMapper.map(rideRequest,Ride.class);
        ride.setRideStatus(RideStatus.CONFIRMED);
        ride.setDriver(driver);
        ride.setOtp(generateRandomOtp());
        ride.setId(null);
        rideRequestService.update(rideRequest);
        return rideRepository.save(ride);

    }

    @Override
    public Ride updateRideStatus(Ride ride, RideStatus rideStatus) {
        ride.setRideStatus(rideStatus);
        return rideRepository.save(ride);

    }

    @Override
    public Page<Ride> getAllRidesOfRider(Long riderId, PageRequest pageRequest) {
        return null;
    }

    @Override
    public Page<Ride> getAllRidesOfDriver(Long driverId, PageRequest pageRequest) {
        return null;
    }

    private String generateRandomOtp(){
        Random random = new Random();
        int otpInt = random.nextInt(10000);
        return String.format("%04d",otpInt);
    }
}
