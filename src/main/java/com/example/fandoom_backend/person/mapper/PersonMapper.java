package com.example.fandoom_backend.person.mapper;

import com.example.fandoom_backend.person.dto.PersonDetailResponse;
import com.example.fandoom_backend.person.dto.PersonSummaryResponse;
import com.example.fandoom_backend.person.entity.Person;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PersonMapper {
    PersonSummaryResponse toSummaryResponse(Person person);
    PersonDetailResponse toDetailResponse(Person person);
    List<PersonSummaryResponse> toSummaryResponseList(List<Person> people);
}
