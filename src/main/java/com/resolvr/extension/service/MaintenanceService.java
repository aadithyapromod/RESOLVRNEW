package com.resolvr.extension.service;

import com.resolvr.dao.VehicleDAO;
import com.resolvr.extension.dao.MaintenanceDAO;
import com.resolvr.extension.model.MaintenanceRecord;
import com.resolvr.model.Vehicle;

import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

public class MaintenanceService {

    private final MaintenanceDAO maintenanceDAO;
    private final VehicleDAO vehicleDAO;

    public MaintenanceService() {
        this.maintenanceDAO = new MaintenanceDAO();
        this.vehicleDAO = new VehicleDAO();
    }

    public MaintenanceService(MaintenanceDAO maintenanceDAO, VehicleDAO vehicleDAO) {
        this.maintenanceDAO = maintenanceDAO;
        this.vehicleDAO = vehicleDAO;
    }

    // Record servicing / repair / maintenance and automatically update vehicle status
    public boolean recordMaintenance(MaintenanceRecord record) throws SQLException {
        if (record == null) {
            throw new IllegalArgumentException("Maintenance record cannot be null.");
        }

        if (record.getVehicleId() <= 0) {
            throw new IllegalArgumentException("A valid vehicle ID is required.");
        }

        Vehicle vehicle = vehicleDAO.getVehicleById(record.getVehicleId());
        if (vehicle == null) {
            throw new IllegalArgumentException("Vehicle #" + record.getVehicleId() + " does not exist.");
        }

        if ("RENTED".equalsIgnoreCase(vehicle.getStatus())) {
            throw new IllegalArgumentException(
                "Vehicle #" + record.getVehicleId() + " is currently RENTED. Complete or return the rental before sending for maintenance."
            );
        }

        if (isBlank(record.getMaintenanceType())) {
            throw new IllegalArgumentException("Maintenance type is required (e.g. Servicing, Repair, Inspection).");
        }

        if (isBlank(record.getDescription())) {
            throw new IllegalArgumentException("Maintenance / servicing details and description are required.");
        }

        if (!Double.isFinite(record.getCost()) || record.getCost() < 0) {
            throw new IllegalArgumentException("Maintenance cost cannot be negative.");
        }

        if (record.getStartDate() == null) {
            throw new IllegalArgumentException("Maintenance start date is required.");
        }

        if (record.getEndDate() != null && record.getEndDate().before(record.getStartDate())) {
            throw new IllegalArgumentException("Completion date cannot be earlier than start date.");
        }

        if (isBlank(record.getStatus())) {
            record.setStatus("UNDER_MAINTENANCE");
        }

        // If condition notes are not provided, pre-populate from vehicle's latest return condition
        if (isBlank(record.getConditionNotes())) {
            String latestCondition = maintenanceDAO.getLatestReturnConditionForVehicle(record.getVehicleId());
            if (!latestCondition.isEmpty()) {
                record.setConditionNotes(latestCondition);
            }
        }

        boolean added = maintenanceDAO.addMaintenance(record);
        if (added) {
            // Update vehicle status based on maintenance state
            if ("UNDER_MAINTENANCE".equalsIgnoreCase(record.getStatus())) {
                vehicleDAO.updateStatus(record.getVehicleId(), "MAINTENANCE");
            } else if ("COMPLETED".equalsIgnoreCase(record.getStatus())) {
                if (!maintenanceDAO.hasActiveMaintenance(record.getVehicleId())) {
                    vehicleDAO.updateStatus(record.getVehicleId(), "AVAILABLE");
                }
            }
        }
        return added;
    }

    // Complete ongoing maintenance, record final cost & remarks, and restore vehicle availability
    public boolean completeMaintenance(int maintenanceId, Date endDate, double finalCost, String completionNotes)
            throws SQLException {
        if (maintenanceId <= 0) {
            throw new IllegalArgumentException("Valid maintenance ID is required.");
        }

        if (endDate == null) {
            throw new IllegalArgumentException("Completion end date is required.");
        }

        if (!Double.isFinite(finalCost) || finalCost < 0) {
            throw new IllegalArgumentException("Final cost cannot be negative.");
        }

        MaintenanceRecord record = maintenanceDAO.getMaintenanceById(maintenanceId);
        if (record == null) {
            throw new IllegalArgumentException("Maintenance record #" + maintenanceId + " not found.");
        }

        if (endDate.before(record.getStartDate())) {
            throw new IllegalArgumentException("Completion date cannot be before maintenance start date (" + record.getStartDate() + ").");
        }

        String notes = isBlank(completionNotes) ? "Servicing & repairs completed" : completionNotes.trim();

        boolean completed = maintenanceDAO.completeMaintenance(maintenanceId, endDate, finalCost, notes);
        if (completed) {
            // Check if there are other ongoing maintenance jobs for this vehicle
            if (!maintenanceDAO.hasActiveMaintenance(record.getVehicleId())) {
                vehicleDAO.updateStatus(record.getVehicleId(), "AVAILABLE");
            }
        }
        return completed;
    }

    // Retrieve all maintenance records
    public List<MaintenanceRecord> getAllMaintenance() throws SQLException {
        return maintenanceDAO.getAllMaintenance();
    }

    // Retrieve maintenance record by ID
    public MaintenanceRecord getMaintenanceById(int maintenanceId) throws SQLException {
        if (maintenanceId <= 0) {
            throw new IllegalArgumentException("Valid maintenance ID is required.");
        }
        return maintenanceDAO.getMaintenanceById(maintenanceId);
    }

    // Retrieve maintenance records for a specific vehicle
    public List<MaintenanceRecord> getMaintenanceByVehicleId(int vehicleId) throws SQLException {
        if (vehicleId <= 0) {
            throw new IllegalArgumentException("Valid vehicle ID is required.");
        }
        return maintenanceDAO.getMaintenanceByVehicleId(vehicleId);
    }

    // Retrieve active maintenance records
    public List<MaintenanceRecord> getActiveMaintenance() throws SQLException {
        return maintenanceDAO.getActiveMaintenance();
    }

    // Retrieve vehicles currently unavailable due to maintenance
    public List<Vehicle> getUnavailableVehiclesDueToMaintenance() throws SQLException {
        return maintenanceDAO.getUnavailableVehiclesDueToMaintenance();
    }

    // Retrieve vehicle condition details from previous returns
    public String getVehicleConditionSummary(int vehicleId) throws SQLException {
        if (vehicleId <= 0) {
            return "";
        }
        return maintenanceDAO.getLatestReturnConditionForVehicle(vehicleId);
    }

    // Update maintenance record
    public boolean updateMaintenance(MaintenanceRecord record) throws SQLException {
        if (record == null || record.getMaintenanceId() <= 0) {
            throw new IllegalArgumentException("Valid maintenance record is required.");
        }

        boolean updated = maintenanceDAO.updateMaintenance(record);
        if (updated) {
            if ("UNDER_MAINTENANCE".equalsIgnoreCase(record.getStatus())) {
                vehicleDAO.updateStatus(record.getVehicleId(), "MAINTENANCE");
            } else if ("COMPLETED".equalsIgnoreCase(record.getStatus())) {
                if (!maintenanceDAO.hasActiveMaintenance(record.getVehicleId())) {
                    vehicleDAO.updateStatus(record.getVehicleId(), "AVAILABLE");
                }
            }
        }
        return updated;
    }

    // Delete maintenance record
    public boolean deleteMaintenance(int maintenanceId) throws SQLException {
        if (maintenanceId <= 0) {
            throw new IllegalArgumentException("Valid maintenance ID is required.");
        }

        MaintenanceRecord record = maintenanceDAO.getMaintenanceById(maintenanceId);
        if (record == null) {
            return false;
        }

        boolean deleted = maintenanceDAO.deleteMaintenance(maintenanceId);
        if (deleted) {
            if (!maintenanceDAO.hasActiveMaintenance(record.getVehicleId())) {
                // If vehicle was marked MAINTENANCE, return it to AVAILABLE
                Vehicle v = vehicleDAO.getVehicleById(record.getVehicleId());
                if (v != null && "MAINTENANCE".equalsIgnoreCase(v.getStatus())) {
                    vehicleDAO.updateStatus(record.getVehicleId(), "AVAILABLE");
                }
            }
        }
        return deleted;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
