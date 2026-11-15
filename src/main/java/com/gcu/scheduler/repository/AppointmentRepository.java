package com.gcu.scheduler.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.gcu.scheduler.model.Appointment;

/**
 * Data access object (DAO) for {@link Appointment} entities.
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    /**
     * @param patientId the patient's user id
     * @return the patient's appointments in date/time order
     */
    @Query("select a from Appointment a where a.patient.id = :patientId "
         + "order by a.timeSlot.slotDate asc, a.timeSlot.startTime asc")
    List<Appointment> findByPatientIdOrdered(@Param("patientId") Long patientId);

    /** @return every appointment in date/time order (admin view) */
    @Query("select a from Appointment a order by a.timeSlot.slotDate asc, a.timeSlot.startTime asc")
    List<Appointment> findAllOrdered();

    /**
     * @param date the day
     * @return appointments on that day in time order (office schedule)
     */
    @Query("select a from Appointment a where a.timeSlot.slotDate = :date order by a.timeSlot.startTime asc")
    List<Appointment> findByDate(@Param("date") LocalDate date);

    /**
     * @param slotId the time slot id
     * @return true if an appointment uses the slot
     */
    boolean existsByTimeSlotId(Long slotId);

    /**
     * @param visitTypeId the visit type id
     * @return true if any appointment uses the visit type
     */
    boolean existsByVisitTypeId(Long visitTypeId);
}
