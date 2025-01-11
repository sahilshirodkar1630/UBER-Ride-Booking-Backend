package com.sahil.project.uber.uberApp.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverDto {
   // private Long id;
    private UserDto user;
    private Double rating;
  //  private Boolean available;
   // Point currentLocation;
}
