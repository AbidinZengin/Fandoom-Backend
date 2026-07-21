package com.example.fandoom_backend.franchise.repository;

import com.example.fandoom_backend.franchise.entity.Franchise;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FranchiseRepository extends JpaRepository<Franchise, Long> {
    Optional<Franchise> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
