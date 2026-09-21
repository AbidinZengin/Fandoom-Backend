package com.example.fandoom_backend.community.repository;

import com.example.fandoom_backend.community.entity.PortalProduction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PortalProductionRepository extends JpaRepository<PortalProduction, Long> {

    List<PortalProduction> findByPortalId(Long portalId);

    // Sayfa başına tek IN sorgusu (liste yanıtlarında productionSlugs için, N+1 yok).
    List<PortalProduction> findByPortalIdIn(Collection<Long> portalIds);

    boolean existsByPortalIdAndProductionSlug(Long portalId, String productionSlug);

    Optional<PortalProduction> findByProductionSlug(String productionSlug);

    // Anında çalışan bulk DELETE (derived deleteBy flush sırasında INSERT'lerden SONRA çalışırdı; aynı transaction'da
    // sil+yeniden ekle unique(production_slug)'a takılırdı).
    // clearAutomatically bilinçli KAPALI: çağıran serviste az önce yüklenen Portal entity'si managed kalmalı.
    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM PortalProduction pp WHERE pp.portalId = :portalId AND pp.productionSlug IN :slugs")
    void deleteByPortalIdAndProductionSlugIn(@Param("portalId") Long portalId, @Param("slugs") Collection<String> slugs);
}
