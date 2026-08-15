package com.example.fandoom_backend.lore.mapper;

import com.example.fandoom_backend.lore.dto.EventParticipantResponse;
import com.example.fandoom_backend.lore.entity.EventParticipant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EventParticipantMapper {

    @Mapping(source = "event.id", target = "eventId")
    EventParticipantResponse toResponse(EventParticipant participant);

    List<EventParticipantResponse> toResponseList(List<EventParticipant> participants);
}
