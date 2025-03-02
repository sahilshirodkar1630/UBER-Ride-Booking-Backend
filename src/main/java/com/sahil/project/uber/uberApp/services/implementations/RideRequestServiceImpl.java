package com.sahil.project.uber.uberApp.services.implementations;

import com.sahil.project.uber.uberApp.entities.RideRequest;
import com.sahil.project.uber.uberApp.exceptions.ResourceNotFoundException;
import com.sahil.project.uber.uberApp.repositories.RideRequestRepository;
import com.sahil.project.uber.uberApp.services.RideRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RideRequestServiceImpl implements RideRequestService  {
    private final RideRequestRepository rideRequestRepository;
    @Override
    public RideRequest findRideRequestById(Long rideRequestId) {
        return rideRequestRepository.findById(rideRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride request not found with id : "+rideRequestId)
        );
    }

    @Override
    public void update(RideRequest rideRequest) {
       rideRequestRepository.findById(rideRequest.getId())
               .orElseThrow(()-> new ResourceNotFoundException("RideRequest not found with id: "+1));

       rideRequestRepository.save(rideRequest);

    }
}
