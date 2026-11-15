package com.gcu.scheduler.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A kind of visit a patient can book (annual checkup, sick visit, follow-up...).
 * Maps to the {@code services} table from the ER diagram. The class is named
 * VisitType rather than Service to avoid clashing with Spring's @Service annotation.
 */
@Entity
@Table(name = "services")
public class VisitType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be 100 characters or fewer")
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Size(max = 255, message = "Description must be 255 characters or fewer")
    @Column(length = 255)
    private String description;

    @Min(value = 10, message = "Duration must be at least 10 minutes")
    @Max(value = 240, message = "Duration must be 240 minutes or fewer")
    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes = 30;

    @Column(nullable = false)
    private boolean active = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
