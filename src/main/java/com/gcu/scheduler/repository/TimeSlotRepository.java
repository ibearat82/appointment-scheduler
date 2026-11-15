package com.gcu.scheduler.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.gcu.scheduler.model.TimeSlot;

import jakarta.persistence.LockModeType;

/**
 * Data access object (DAO) for {@link TimeSlot} entities.
 */
@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    /**
     * @param date the day to search
     * @return open slots on that day, earliest first
     */
    List<TimeSlot> findBySlotDateAndAvailableTrueOrderByStartTimeAsc(LocalDate date);

    /**
     * @param from first day (inclusive)
     * @param to   last day (inclusive)
     * @return open slots in the range, in date/time order
     */
    List<TimeSlot> findBySlotDateBetweenAndAvailableTrueOrderBySlotDateAscStartTimeAsc(LocalDate from, LocalDate to);

    /**
     * @param from first day (inclusive)
     * @return all slots from that day on, in date/time order (admin view)
     */
    List<TimeSlot> findBySlotDateGreaterThanEqualOrderBySlotDateAscStartTimeAsc(LocalDate from);

    /**
     * @param date      the day
     * @param startTime the start time
     * @return true if a slot already starts at that date and time
     */
    boolean existsBySlotDateAndStartTime(LocalDate date, LocalTime startTime);

    /**
     * Loads a slot and locks its row until the transaction ends, so two
     * patients cannot book the same slot at the same moment.
     *
     * @param id the slot id
     * @return the locked slot, if it exists
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from TimeSlot s where s.id = :id")
    Optional<TimeSlot> findByIdForUpdate(@Param("id") Long id);
}
