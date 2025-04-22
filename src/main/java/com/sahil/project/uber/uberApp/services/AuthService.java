package com.sahil.project.uber.uberApp.services;

import com.sahil.project.uber.uberApp.dto.DriverDto;
import com.sahil.project.uber.uberApp.dto.SignupDto;
import com.sahil.project.uber.uberApp.dto.UserDto;
import com.sahil.project.uber.uberApp.entities.Driver;

public interface AuthService{
    String login(String email,String password);
    UserDto signup(SignupDto signupDto);
    DriverDto onboardNewDriver(Long userId,String vehicleId);

}
