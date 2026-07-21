package com.example.fandoom_backend.genre.entity;

import com.example.fandoom_backend.common.entity.Auditable;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "genre", uniqueConstraints = {
        @UniqueConstraint(name = "uk_genre_name", columnNames = "name"),
        @UniqueConstraint(name = "uk_genre_slug", columnNames = "slug")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString
public class Genre extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Column(name = "slug", nullable = false, length = 100)
    private String slug;
}
