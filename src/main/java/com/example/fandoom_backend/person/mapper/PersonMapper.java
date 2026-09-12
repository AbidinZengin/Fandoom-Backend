package com.example.fandoom_backend.person.mapper;

import com.example.fandoom_backend.common.util.LocalizedTextResolver;
import com.example.fandoom_backend.person.dto.PersonDetailResponse;
import com.example.fandoom_backend.person.dto.PersonSummaryResponse;
import com.example.fandoom_backend.person.entity.Person;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", imports = LocalizedTextResolver.class)
public interface PersonMapper {
    PersonSummaryResponse toSummaryResponse(Person person);

    @Mapping(target = "bio", expression = "java(LocalizedTextResolver.resolve(person.getBioTr(), person.getBio()))")
    PersonDetailResponse toDetailResponse(Person person);

    List<PersonSummaryResponse> toSummaryResponseList(List<Person> people);
}
