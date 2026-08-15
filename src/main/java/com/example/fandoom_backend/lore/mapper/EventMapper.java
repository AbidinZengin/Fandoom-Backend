package com.example.fandoom_backend.lore.mapper;

import com.example.fandoom_backend.lore.dto.EventResponse;
import com.example.fandoom_backend.lore.entity.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EventMapper {

    @Mapping(source = "location.id", target = "locationId")
    @Mapping(source = "location.name", target = "locationName")
    @Mapping(source = "location.slug", target = "locationSlug")
    EventResponse toResponse(Event event);

    List<EventResponse> toResponseList(List<Event> events);
}
