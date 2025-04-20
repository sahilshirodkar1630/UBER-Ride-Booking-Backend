package com.sahil.project.uber.uberApp.services.implementations;

import com.sahil.project.uber.uberApp.dto.DriverDto;
import com.sahil.project.uber.uberApp.dto.RideDto;
import com.sahil.project.uber.uberApp.dto.RiderDto;
import com.sahil.project.uber.uberApp.entities.Driver;
import com.sahil.project.uber.uberApp.entities.Ride;
import com.sahil.project.uber.uberApp.entities.RideRequest;
import com.sahil.project.uber.uberApp.entities.enums.RideRequestStatus;
import com.sahil.project.uber.uberApp.entities.enums.RideStatus;
import com.sahil.project.uber.uberApp.exceptions.ResourceNotFoundException;
import com.sahil.project.uber.uberApp.repositories.DriverRepository;
import com.sahil.project.uber.uberApp.services.DriverService;
import com.sahil.project.uber.uberApp.services.PaymentService;
import com.sahil.project.uber.uberApp.services.RideRequestService;
import com.sahil.project.uber.uberApp.services.RideService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class DriverServiceImpl implements DriverService {

    private final RideRequestService rideRequestService;
    private final DriverRepository driverRepository;
    private final RideService rideService;
    private final PaymentService paymentService;
    private final ModelMapper modelMapper;
    @Override
    @Transactional
    public RideDto acceptRide(Long rideRequestId) {
        Driver driver = getCurrentDriver();
        RideRequest rideRequest = rideRequestService.findRideRequestById(rideRequestId);

        if(!rideRequest.getRideRequestStatus().equals(RideRequestStatus.PENDING)){
            throw new RuntimeException("RideRequest cannot be accepted, status is "+rideRequest.getRideRequestStatus());
        }

        if(!driver.getAvailable()){
            throw new RuntimeException("Driver cannot accept ride due to unavailability");
        }

        Driver savedDriver = updateDriverAvailability(driver,false);

        Ride ride = rideService.createNewRide(rideRequest,savedDriver);
        return modelMapper.map(ride,RideDto.class) ;
    }

    @Override
    public RideDto cancelRide(Long rideId) {
        Ride ride = rideService.getRideById(rideId);

        Driver driver = getCurrentDriver();
        if(!driver.equals(ride.getDriver())){
            throw new RuntimeException("Driver cannot cancel a ride as he has not accepted it earlier");
        }

        if(!ride.getRideStatus().equals(RideStatus.CONFIRMED)){
            throw new RuntimeException("Driver cannot be cancelled, invalid status: "+ride.getRideStatus());
        }

        Ride savedRide = rideService.updateRideStatus(ride,RideStatus.CANCELLED);

        updateDriverAvailability(driver,true);
        return modelMapper.map(savedRide,RideDto.class);

    }

    @Override
    public RideDto startRide(Long rideId,String otp) {
        Ride ride = rideService.getRideById(rideId);
        Driver driver = getCurrentDriver();
        if(!driver.equals(ride.getDriver())){
            throw new RuntimeException("Driver cannot start a ride as he has not accepted it earlier");
        }

        if(!ride.getRideStatus().equals(RideStatus.CONFIRMED)){
            throw new RuntimeException("Ride status is not CONFIRMED hence cannot be started, status: "+ride.getRideStatus());
        }
        if(!otp.equals(ride.getOtp())){
            throw new RuntimeException("Otp is not valid, otp: "+ride.getOtp());
        }

        ride.setStartedAt(LocalDateTime.now());
        Ride savedRide = rideService.updateRideStatus(ride,RideStatus.ONGOING);

        paymentService.createNewPayment(ride);
        return modelMapper.map(savedRide,RideDto.class);

    }

    @Override
    @Transactional
    public RideDto endRide(Long rideId) {
        Ride ride = rideService.getRideById(rideId);
        Driver driver = getCurrentDriver();
        if(!driver.equals(ride.getDriver())){
            throw new RuntimeException("Driver cannot start a ride as he has not accepted it earlier");
        }

        if(!ride.getRideStatus().equals(RideStatus.ONGOING)){
            throw new RuntimeException("Ride status is not ONGOING hence cannot be started, status: "+ride.getRideStatus());
        }

        ride.setEndedAt(LocalDateTime.now());
        Ride savedRide =rideService.updateRideStatus(ride,RideStatus.ENDED);

        updateDriverAvailability(driver,true);
        paymentService.processPayment(ride);

        return modelMapper.map(savedRide,RideDto.class);
    }

    @Override
    public RiderDto rateRider(Long rideId, Integer rating) {
        return null;
    }

    @Override
    public DriverDto getMyProfile() {
        Driver currentDriver  = getCurrentDriver();
        return modelMapper.map(currentDriver,DriverDto.class);
    }

    @Override
    public Page<RideDto> getAllMyRides(PageRequest pageRequest) {
        Driver currentDriver  = getCurrentDriver();
        return rideService.getAllRidesOfDriver(currentDriver, pageRequest).map(
                ride-> modelMapper.map(ride,RideDto.class)
        );
    }

    @Override
    public Driver getCurrentDriver() {
        return driverRepository.findById(2L)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id : "+2));
    }

    @Override
    public Driver updateDriverAvailability(Driver driver, boolean availability) {
        driver.setAvailable(availability);
       return driverRepository.save(driver);
    }
}
