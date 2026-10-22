package com.resolvr.service;

import com.resolvr.dao.RentalDAO;
import com.resolvr.dao.ReturnDAO;
import com.resolvr.dao.VehicleDAO;
import com.resolvr.extension.dao.MaintenanceDAO;
import com.resolvr.extension.model.MaintenanceRecord;
import com.resolvr.model.Rental;
import com.resolvr.model.VehicleReturn;

import java.sql.SQLException;
import java.util.List;

public class ReturnService {

    private final ReturnDAO returnDAO;
    private final RentalDAO rentalDAO;
    private final VehicleDAO vehicleDAO;

    public ReturnService() {
        this.returnDAO = new ReturnDAO();
        this.rentalDAO = new RentalDAO();
        this.vehicleDAO = new VehicleDAO();
    }

    // Record a vehicle return
    public boolean addReturn(VehicleReturn vehicleReturn)
            throws SQLException {

        if (vehicleReturn == null) {
            throw new IllegalArgumentException(
                    "Return record cannot be null."
            );
        }

        if (vehicleReturn.getRentalId() <= 0) {
            throw new IllegalArgumentException(
                    "Valid rental ID is required."
            );
        }

        if (vehicleReturn.getActualReturnDate() == null) {
            throw new IllegalArgumentException(
                    "Actual return date is required."
            );
        }

        if (!Double.isFinite(vehicleReturn.getDamageCharge())
                || vehicleReturn.getDamageCharge() < 0) {
            throw new IllegalArgumentException(
                    "Damage charge cannot be negative."
            );
        }

        if (!Double.isFinite(vehicleReturn.getRefundAmount())
                || vehicleReturn.getRefundAmount() < 0) {
            throw new IllegalArgumentException(
                    "Refund amount cannot be negative."
            );
        }

        Rental rental =
                rentalDAO.getRentalById(vehicleReturn.getRentalId());

        if (rental == null) {
            throw new IllegalArgumentException(
                    "The specified rental does not exist."
            );
        }

        if ("COMPLETED".equalsIgnoreCase(rental.getStatus())
                || "CANCELLED".equalsIgnoreCase(rental.getStatus())
                || "RETURNED".equalsIgnoreCase(rental.getStatus())) {
            throw new IllegalArgumentException(
                    "This rental is already completed, cancelled or returned."
            );
        }

        return returnDAO.addReturn(vehicleReturn);
    }

    // Complete return workflow: adds return record, completes rental, updates vehicle status according to condition
    public boolean processReturn(VehicleReturn vehicleReturn) throws SQLException {
        if (vehicleReturn == null) {
            throw new IllegalArgumentException("Return record cannot be null.");
        }

        Rental rental = rentalDAO.getRentalById(vehicleReturn.getRentalId());
        if (rental == null) {
            throw new IllegalArgumentException("The specified rental does not exist.");
        }

        boolean success = addReturn(vehicleReturn);
        if (success) {
            rentalDAO.updateRentalStatus(rental.getRentalId(), "COMPLETED");

            // Evaluate vehicle condition and damage
            boolean hasDamage = vehicleReturn.getDamageCharge() > 0;
            String notes = vehicleReturn.getConditionNotes();
            boolean damageInNotes = isDamagedCondition(notes);

            if (hasDamage || damageInNotes) {
                // Vehicle condition requires servicing/repairs -> mark as MAINTENANCE (unavailable)
                vehicleDAO.updateStatus(rental.getVehicleId(), "MAINTENANCE");

                // Auto-create initial maintenance record in the extension module
                try {
                    MaintenanceDAO maintenanceDAO = new MaintenanceDAO();
                    String mType = hasDamage ? "Accident / Crash Repair" : "Servicing & Repair Inspection";
                    String desc = "Auto-flagged from return #" + vehicleReturn.getReturnId() +
                            (notes != null && !notes.isEmpty() ? ": " + notes : " (Damage reported)");
                    MaintenanceRecord mRecord = new MaintenanceRecord(
                            rental.getVehicleId(),
                            mType,
                            desc,
                            vehicleReturn.getDamageCharge(),
                            vehicleReturn.getActualReturnDate(),
                            null,
                            "UNDER_MAINTENANCE",
                            "Workshop Inspection Pending",
                            notes != null ? notes : ""
                    );
                    maintenanceDAO.addMaintenance(mRecord);
                } catch (Exception ex) {
                    System.err.println("Could not auto-create maintenance record: " + ex.getMessage());
                }
            } else {
                // Returned in good condition -> restore to AVAILABLE
                vehicleDAO.updateStatus(rental.getVehicleId(), "AVAILABLE");
            }
        }
        return success;
    }

    private boolean isDamagedCondition(String notes) {
        if (notes == null || notes.trim().isEmpty()) return false;
        String lower = notes.toLowerCase();
        return lower.contains("damage")
                || lower.contains("repair")
                || lower.contains("crash")
                || lower.contains("accident")
                || lower.contains("broken")
                || lower.contains("issue")
                || lower.contains("dent")
                || lower.contains("scratch")
                || lower.contains("fault")
                || lower.contains("puncture")
                || lower.contains("leak");
    }

    // Retrieve all return records
    public List<VehicleReturn> getAllReturns() throws SQLException {
        return returnDAO.getAllReturns();
    }

    // Retrieve a return record by rental ID
    public VehicleReturn getReturnByRentalId(int rentalId)
            throws SQLException {

        if (rentalId <= 0) {
            throw new IllegalArgumentException(
                    "Invalid rental ID."
            );
        }

        return returnDAO.getReturnByRentalId(rentalId);
    }

    // Retrieve return records for a customer
    public List<VehicleReturn> getReturnsByCustomerId(int customerId) throws SQLException {
        if (customerId <= 0) {
            throw new IllegalArgumentException("Invalid customer ID.");
        }
        return returnDAO.getReturnsByCustomerId(customerId);
    }

    // Retrieve a return record by return ID
    public VehicleReturn getReturnById(int returnId) throws SQLException {
        if (returnId <= 0) {
            throw new IllegalArgumentException("Invalid return ID.");
        }
        return returnDAO.getReturnById(returnId);
    }
}
