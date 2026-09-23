package com.example.fandoom_backend.movie.mapper;

import com.example.fandoom_backend.common.util.LocalizedTextResolver;
import com.example.fandoom_backend.movie.dto.MovieDetailResponse;
import com.example.fandoom_backend.movie.dto.MovieSummaryResponse;
import com.example.fandoom_backend.movie.entity.Movie;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", imports = LocalizedTextResolver.class)
public interface MovieMapper {

    @Mapping(target = "title", expression = "java(LocalizedTextResolver.resolve(movie.getTitleTr(), movie.getTitle()))")
    MovieSummaryResponse toSummaryResponse(Movie movie);

    @Mapping(target = "title", expression = "java(LocalizedTextResolver.resolve(movie.getTitleTr(), movie.getTitle()))")
    @Mapping(target = "synopsis", expression = "java(LocalizedTextResolver.resolve(movie.getSynopsisTr(), movie.getSynopsis()))")
    @Mapping(target = "tagline", expression = "java(LocalizedTextResolver.resolve(movie.getTaglineTr(), movie.getTagline()))")
    MovieDetailResponse toDetailResponse(Movie movie);

    List<MovieSummaryResponse> toSummaryResponseList(List<Movie> movies);
}
