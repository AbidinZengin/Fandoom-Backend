package com.example.fandoom_backend.series.repository;

import com.example.fandoom_backend.series.entity.Season;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SeasonRepository extends JpaRepository<Season, Long> {

    // Sezon detayı: episodes tek JOIN FETCH. seasonBlocks ve episodeBlocks de birer List (bag) olduğundan
    // aynı grafta çekilemez (MultipleBagFetchException); onlar default_batch_fetch_size ile
    // IN(...) toplu sorgularıyla gelir (bölüm başına ayrı sorgu YOK). Collection fetch + Pageable
    // KULLANILMAZ (HHH000104 in-memory paging) — bu yalnızca tekil kayıt sorgusudur.
    @EntityGraph(attributePaths = "episodes")
    Optional<Season> findWithEpisodesById(Long id);

    List<Season> findBySeriesIdOrderBySeasonNumberAsc(Long seriesId);
}
