package com.sahil.project.uber.uberApp.advices;
import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
@Builder
public class ApiError {
    private HttpStatus httpStatus;
    private String errorMessage;
}
