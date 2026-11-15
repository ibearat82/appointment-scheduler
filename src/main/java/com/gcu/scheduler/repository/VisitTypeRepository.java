package com.gcu.scheduler.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gcu.scheduler.model.VisitType;

/**
 * Data access object (DAO) for {@link VisitType} entities (services table).
 */
@Repository
public interface VisitTypeRepository extends JpaRepository<VisitType, Long> {

    /** @return active visit types, sorted by name */
    List<VisitType> findByActiveTrueOrderByNameAsc();

    /** @return all visit types, sorted by name */
    List<VisitType> findAllByOrderByNameAsc();

    /**
     * @param name the name to check
     * @return true if a visit type already uses the name
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * @param name the name to check
     * @param id   the id to exclude (the record being edited)
     * @return true if another visit type uses the name
     */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
