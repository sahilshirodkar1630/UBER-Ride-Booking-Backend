package com.sahil.project.uber.uberApp.services.implementations;

import com.sahil.project.uber.uberApp.dto.DriverDto;
import com.sahil.project.uber.uberApp.dto.RideDto;
import com.sahil.project.uber.uberApp.dto.RideRequestDto;
import com.sahil.project.uber.uberApp.dto.RiderDto;
import com.sahil.project.uber.uberApp.entities.*;
import com.sahil.project.uber.uberApp.entities.enums.RideRequestStatus;
import com.sahil.project.uber.uberApp.entities.enums.RideStatus;
import com.sahil.project.uber.uberApp.exceptions.ResourceNotFoundException;
import com.sahil.project.uber.uberApp.repositories.RideRequestRepository;
import com.sahil.project.uber.uberApp.repositories.RiderRepository;
import com.sahil.project.uber.uberApp.services.DriverService;
import com.sahil.project.uber.uberApp.services.RideService;
import com.sahil.project.uber.uberApp.services.RiderService;
import com.sahil.project.uber.uberApp.strategies.RideStrategyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Service
@RequiredArgsConstructor
@Slf4j
public class RiderServiceImpl implements RiderService {
    private final ModelMapper modelMapper;
    private final RideStrategyManager rideStrategyManager;
    private final RideRequestRepository rideRequestRepository;
    private final RiderRepository riderRepository;
    private final RideService rideService;
    private final DriverService driverService;

    @Override
    @Transactional
    public RideRequestDto requestRide(RideRequestDto rideRequestDto) {
        Rider rider = getCurrentRider();
        RideRequest rideRequest = modelMapper.map(rideRequestDto,RideRequest.class);
        rideRequest.setRideRequestStatus(RideRequestStatus.PENDING);
        rideRequest.setRider(rider);

        //Calculate Ride Fare based on pickup and drop location
        Double fare = rideStrategyManager.rideFareCalculationStrategy().calculateFare(rideRequest);
        rideRequest.setFare(fare);

        RideRequest savedRideRequest = rideRequestRepository.save(rideRequest);

        // Get Available & Matching drivers list
        List<Driver> drivers = rideStrategyManager
                .driverMatchingStrategy(getCurrentRider().getRating())
                .findMatchingDriver(rideRequest);

        // TODO : Send notifications to all drivers about this ride request
        System.out.println(rideRequest.toString());
        return modelMapper.map(savedRideRequest,RideRequestDto.class);
    }

    @Override
    public RideDto cancelRide(Long rideId) {
        Rider rider = getCurrentRider();
        Ride ride = rideService.getRideById(rideId);
        if(!rider.equals(ride.getRider())){
            throw new RuntimeException("Rider cannot cancel a ride as he has not accepted it earlier");
        }

        if(!ride.getRideStatus().equals(RideStatus.CONFIRMED)){
            throw new RuntimeException("Rider cannot be cancelled, invalid status: "+ride.getRideStatus());
        }

        Ride savedRide = rideService.updateRideStatus(ride,RideStatus.CANCELLED);

        driverService.updateDriverAvailability(ride.getDriver(),true);
        return modelMapper.map(savedRide,RideDto.class);
    }

    @Override
    public DriverDto rateDriver(Long rideId, Integer rating) {
        return null;
    }

    @Override
    public RiderDto getMyProfile() {
        Rider rider = getCurrentRider();
        return modelMapper.map(rider,RiderDto.class);
    }

    @Override
    public Page<RideDto> getAllMyRides(PageRequest pageRequest) {
        Rider rider = getCurrentRider();
        return rideService.getAllRidesOfRider(rider, pageRequest).map(
                ride-> modelMapper.map(ride,RideDto.class)
        );
    }

    @Override
    public Rider createRider(User user) {
        Rider rider = Rider
                .builder()
                .user(user)
                .rating(0.0)
                .build();
        return riderRepository.save(rider);
    }

    @Override
    public Rider getCurrentRider() {
        // TODO : implement Spring Security
        return riderRepository.findById(1L).orElseThrow(()-> new ResourceNotFoundException("Rider not found with id: "+1));
    }
}
