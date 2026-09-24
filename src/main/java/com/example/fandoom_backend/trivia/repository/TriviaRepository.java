package com.example.fandoom_backend.trivia.repository;

import com.example.fandoom_backend.trivia.entity.Trivia;
import com.example.fandoom_backend.trivia.entity.TriviaItemType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TriviaRepository extends JpaRepository<Trivia, Long> {

    List<Trivia> findByItemIdAndItemType(Long itemId, TriviaItemType itemType, Pageable pageable);

    // Rastgele sıralama DB seviyesinde. Sorgu (item_id, item_type) indeksiyle bir yapımın
    // trivia'larına daralır, RAND() yalnızca o küçük küme üzerinde çalışır.
    // MySQL'e özgü (RAND); JPQL'de taşınabilir karşılığı yok.
    @Query(value = "SELECT * FROM production_trivia "
            + "WHERE item_id = :itemId AND item_type = :itemType "
            + "ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<Trivia> findRandom(@Param("itemId") Long itemId,
                            @Param("itemType") String itemType,
                            @Param("limit") int limit);
}
