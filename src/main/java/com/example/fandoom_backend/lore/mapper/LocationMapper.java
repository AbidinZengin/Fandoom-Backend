package com.example.fandoom_backend.lore.mapper;

import com.example.fandoom_backend.lore.dto.LocationResponse;
import com.example.fandoom_backend.lore.entity.Location;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LocationMapper {
    LocationResponse toResponse(Location location);
    List<LocationResponse> toResponseList(List<Location> locations);
}
