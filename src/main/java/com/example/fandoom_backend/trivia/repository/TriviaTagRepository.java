package com.example.fandoom_backend.trivia.repository;

import com.example.fandoom_backend.trivia.entity.TriviaTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TriviaTagRepository extends JpaRepository<TriviaTag, Long> {

    Optional<TriviaTag> findByCodeIgnoreCase(String code);

    // Tablo küçük (birkaç on satır); ekleme sırası gösterim sırasıdır.
    List<TriviaTag> findAllByOrderByIdAsc();
}
