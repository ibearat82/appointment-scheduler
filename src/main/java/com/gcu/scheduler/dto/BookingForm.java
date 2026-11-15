package com.gcu.scheduler.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Form backing object for booking or rescheduling an appointment.
 */
public class BookingForm {

    @NotNull(message = "Please choose a visit type")
    private Long visitTypeId;

    @NotNull(message = "Please choose a time slot")
    private Long slotId;

    @Size(max = 255, message = "Notes must be 255 characters or fewer")
    private String notes;

    public Long getVisitTypeId() { return visitTypeId; }
    public void setVisitTypeId(Long visitTypeId) { this.visitTypeId = visitTypeId; }

    public Long getSlotId() { return slotId; }
    public void setSlotId(Long slotId) { this.slotId = slotId; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
