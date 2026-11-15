package com.gcu.scheduler.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gcu.scheduler.exception.BookingException;
import com.gcu.scheduler.exception.ResourceNotFoundException;
import com.gcu.scheduler.model.VisitType;
import com.gcu.scheduler.repository.AppointmentRepository;
import com.gcu.scheduler.repository.VisitTypeRepository;

/**
 * Business service for visit types (the services offered by the office).
 */
@Service
public class VisitTypeService {

    private static final Logger log = LoggerFactory.getLogger(VisitTypeService.class);

    private final VisitTypeRepository visitTypeRepository;
    private final AppointmentRepository appointmentRepository;

    /**
     * @param visitTypeRepository   visit type DAO (injected)
     * @param appointmentRepository appointment DAO (injected)
     */
    public VisitTypeService(VisitTypeRepository visitTypeRepository, AppointmentRepository appointmentRepository) {
        this.visitTypeRepository = visitTypeRepository;
        this.appointmentRepository = appointmentRepository;
    }

    /** @return visit types patients can book */
    @Transactional(readOnly = true)
    public List<VisitType> findActive() {
        return visitTypeRepository.findByActiveTrueOrderByNameAsc();
    }

    /** @return all visit types, including inactive ones (admin view) */
    @Transactional(readOnly = true)
    public List<VisitType> findAll() {
        return visitTypeRepository.findAllByOrderByNameAsc();
    }

    /**
     * @param id the visit type id
     * @return the visit type
     * @throws ResourceNotFoundException if it does not exist
     */
    @Transactional(readOnly = true)
    public VisitType findById(Long id) {
        return visitTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Visit type", id));
    }

    /**
     * Creates or updates a visit type, keeping names unique.
     *
     * @param visitType validated visit type (id is null for a new one)
     * @return the saved visit type
     * @throws BookingException if another visit type already has the name
     */
    @Transactional
    public VisitType save(VisitType visitType) {
        log.info("Entering save() for visit type '{}'", visitType.getName());
        String name = visitType.getName().trim();
        boolean duplicate = visitType.getId() == null
                ? visitTypeRepository.existsByNameIgnoreCase(name)
                : visitTypeRepository.existsByNameIgnoreCaseAndIdNot(name, visitType.getId());
        if (duplicate) {
            throw new BookingException("A visit type named '" + name + "' already exists.");
        }
        visitType.setName(name);
        VisitType saved = visitTypeRepository.save(visitType);
        log.info("Exiting save() - visit type id {}", saved.getId());
        return saved;
    }

    /**
     * Deletes a visit type that has never been booked; otherwise deactivates it
     * so existing appointments keep their history.
     *
     * @param id the visit type id
     * @return true if deleted, false if deactivated instead
     */
    @Transactional
    public boolean deleteOrDeactivate(Long id) {
        VisitType visitType = findById(id);
        if (appointmentRepository.existsByVisitTypeId(id)) {
            visitType.setActive(false);
            visitTypeRepository.save(visitType);
            log.info("Visit type {} is in use, so it was deactivated", id);
            return false;
        }
        visitTypeRepository.delete(visitType);
        log.info("Visit type {} deleted", id);
        return true;
    }
}
