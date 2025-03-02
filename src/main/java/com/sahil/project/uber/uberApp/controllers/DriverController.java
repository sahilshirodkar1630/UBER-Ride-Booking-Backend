package com.sahil.project.uber.uberApp.controllers;

import com.sahil.project.uber.uberApp.dto.RideDto;
import com.sahil.project.uber.uberApp.dto.RideStartDto;
import com.sahil.project.uber.uberApp.services.DriverService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/drivers")
public class DriverController {

    private final DriverService driverService;
    @PostMapping("/acceptRide/{rideRequestId}")
    public ResponseEntity<RideDto> acceptRide(@PathVariable Long rideRequestId){
        return ResponseEntity.ok(driverService.acceptRide(rideRequestId));
    }

    @PostMapping("/startRide")
    public ResponseEntity<RideDto> startRide(@RequestBody RideStartDto rideStartDto){
        return ResponseEntity.ok(
                driverService.startRide(rideStartDto.getRideId(), rideStartDto.getOtp())
        );
    }

}
