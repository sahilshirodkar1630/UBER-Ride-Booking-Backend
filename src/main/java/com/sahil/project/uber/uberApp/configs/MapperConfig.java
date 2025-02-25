package com.sahil.project.uber.uberApp.configs;

import com.sahil.project.uber.uberApp.dto.PointDto;
import com.sahil.project.uber.uberApp.utils.GeometryUtil;
import org.locationtech.jts.geom.Point;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ui.ModelMap;

import java.awt.*;

@Configuration
public class MapperConfig {
    @Bean
    public ModelMapper modelMapper(){
        ModelMapper mapper = new ModelMapper();
        mapper.typeMap(PointDto.class, Point.class).setConverter(converter->{
            PointDto pointDto = converter.getSource();
            return GeometryUtil.createPoint(pointDto);
        });

        mapper.typeMap(Point.class, PointDto.class).setConverter(context->{
            Point point = context.getSource();
            double coordinates[] = {
                    point.getX(),
                    point.getY()
            };
            return new PointDto(coordinates);
        });
        return mapper;
    }
}
