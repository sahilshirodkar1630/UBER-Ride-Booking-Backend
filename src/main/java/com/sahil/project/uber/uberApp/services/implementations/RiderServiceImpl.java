package com.sahil.project.uber.uberApp.services.implementations;

import com.sahil.project.uber.uberApp.dto.DriverDto;
import com.sahil.project.uber.uberApp.dto.RideDto;
import com.sahil.project.uber.uberApp.dto.RideRequestDto;
import com.sahil.project.uber.uberApp.dto.RiderDto;
import com.sahil.project.uber.uberApp.entities.Driver;
import com.sahil.project.uber.uberApp.entities.RideRequest;
import com.sahil.project.uber.uberApp.entities.Rider;
import com.sahil.project.uber.uberApp.entities.User;
import com.sahil.project.uber.uberApp.entities.enums.RideRequestStatus;
import com.sahil.project.uber.uberApp.exceptions.ResourceNotFoundException;
import com.sahil.project.uber.uberApp.repositories.RideRequestRepository;
import com.sahil.project.uber.uberApp.repositories.RiderRepository;
import com.sahil.project.uber.uberApp.services.RiderService;
import com.sahil.project.uber.uberApp.strategies.RideStrategyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
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

    @Override
    @Transactional
    public RideRequestDto requestRide(RideRequestDto rideRequestDto) {
        Rider rider = getCurrentRider();
        RideRequest rideRequest = modelMapper.map(rideRequestDto,RideRequest.class);
        rideRequest.setRideRequestStatus(RideRequestStatus.PENDING);
        rideRequest.setRider(rider);

        Double fare = rideStrategyManager.rideFareCalculationStrategy().calculateFare(rideRequest);
        rideRequest.setFare(fare);

        RideRequest savedRideRequest = rideRequestRepository.save(rideRequest);

        List<Driver> drivers = rideStrategyManager
                .driverMatchingStrategy(getCurrentRider().getRating())
                .findMatchingDriver(rideRequest);

        // TODO : Send notifications to all drivers about this ride request
        System.out.println(rideRequest.toString());
        return modelMapper.map(savedRideRequest,RideRequestDto.class);
    }

    @Override
    public RideDto cancelRide(Long rideId) {
        return null;
    }

    @Override
    public DriverDto rateDriver(Long rideId, Integer rating) {
        return null;
    }

    @Override
    public RiderDto getMyProfile() {
        return null;
    }

    @Override
    public List<RideDto> getAllMyRides() {
        return null;
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
