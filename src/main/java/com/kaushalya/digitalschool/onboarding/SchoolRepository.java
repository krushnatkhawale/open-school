package com.kaushalya.digitalschool.onboarding;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SchoolRepository extends JpaRepository<School, UUID> {

    @Query("SELECT s FROM School s WHERE LOWER(s.name) = LOWER(:name)")
    Optional<School> findByNameIgnoreCase(@Param("name") String name);

    List<School> findAllByOrderByNameAsc();
}
