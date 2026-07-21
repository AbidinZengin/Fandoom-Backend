package com.example.fandoom_backend.series.repository;

import com.example.fandoom_backend.series.entity.Season;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeasonRepository extends JpaRepository<Season, Long> {
    List<Season> findBySeriesIdOrderBySeasonNumberAsc(Long seriesId);
}
