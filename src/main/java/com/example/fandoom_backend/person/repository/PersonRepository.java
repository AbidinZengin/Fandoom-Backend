package com.example.fandoom_backend.person.repository;

import com.example.fandoom_backend.person.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PersonRepository extends JpaRepository<Person, Long> {
    Optional<Person> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
