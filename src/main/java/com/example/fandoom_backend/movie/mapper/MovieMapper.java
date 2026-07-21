package com.example.fandoom_backend.movie.mapper;

import com.example.fandoom_backend.movie.dto.MovieDetailResponse;
import com.example.fandoom_backend.movie.dto.MovieSummaryResponse;
import com.example.fandoom_backend.movie.entity.Movie;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MovieMapper {
    MovieSummaryResponse toSummaryResponse(Movie movie);
    MovieDetailResponse toDetailResponse(Movie movie);
    List<MovieSummaryResponse> toSummaryResponseList(List<Movie> movies);
}
