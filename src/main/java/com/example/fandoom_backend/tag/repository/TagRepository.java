package com.example.fandoom_backend.tag.repository;

import com.example.fandoom_backend.tag.dto.TagFacetOptionResponse;
import com.example.fandoom_backend.tag.entity.Tag;
import com.example.fandoom_backend.tag.entity.TagType;
import com.example.fandoom_backend.tag.entity.TaggableType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, Long id);
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);
    List<Tag> findByType(TagType type);

    // Blog hub facet paneli: verilen TagType'a (FORMAT/MOOD/THEME) ait her
    // tag için, verilen taggableType'a (BLOG) atanma sayısı. LEFT JOIN, hiç
    // atanmamış (count=0) tag'lerin de listeye dahil olmasını sağlar — bir
    // facet seçeneği, henüz hiçbir blog'a atanmamış olsa bile panelde
    // "0" sayaçla görünmeli. JPQL constructor expression (`new ...DTO(...)`)
    // COUNT gibi agregasyon fonksiyonlarını doğrudan constructor argümanı
    // olarak kabul eder, Hibernate/JPA'da standart ve desteklenen bir
    // projeksiyon şeklidir.
    @Query("SELECT new com.example.fandoom_backend.tag.dto.TagFacetOptionResponse(t.id, t.name, t.slug, COUNT(ta.id)) "
            + "FROM Tag t LEFT JOIN TagAssignment ta ON ta.tag = t AND ta.taggableType = :taggableType "
            + "WHERE t.type = :type GROUP BY t.id, t.name, t.slug")
    List<TagFacetOptionResponse> findFacetOptions(
            @Param("type") TagType type, @Param("taggableType") TaggableType taggableType);
}
