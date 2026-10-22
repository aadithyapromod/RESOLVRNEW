package com.resolvr;

import com.resolvr.extension.dao.MaintenanceDAO;
import com.resolvr.extension.model.MaintenanceRecord;
import com.resolvr.extension.service.MaintenanceService;
import com.resolvr.model.Rental;
import com.resolvr.model.Vehicle;
import com.resolvr.model.VehicleReturn;
import com.resolvr.service.RentalService;
import com.resolvr.service.ReturnService;
import com.resolvr.service.VehicleService;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

public class MaintenanceTest {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("    RESOLVR VEHICLE MAINTENANCE EXTENSION TEST   ");
        System.out.println("==================================================");

        VehicleService vehicleService = new VehicleService();
        MaintenanceService maintenanceService = new MaintenanceService();
        RentalService rentalService = new RentalService();
        ReturnService returnService = new ReturnService();

        try {
            // Step 1: Ensure we have a test vehicle
            List<Vehicle> allVehicles = vehicleService.getAllVehicles();
            if (allVehicles.isEmpty()) {
                System.out.println("No vehicles found to test.");
                return;
            }

            Vehicle testVehicle = allVehicles.get(0);
            int vId = testVehicle.getVehicleId();
            System.out.println("\n[Test 1] Selected Test Vehicle: #" + vId + " (" + testVehicle.getModel() +
                    " - " + testVehicle.getRegistrationNo() + ") Initial Status: " + testVehicle.getStatus());

            // Reset status to AVAILABLE if needed for test
            vehicleService.updateStatus(vId, "AVAILABLE");

            // Step 2: Record a Maintenance / Servicing Job
            System.out.println("\n[Test 2] Recording Servicing & Repair for Vehicle #" + vId);
            Date startDate = Date.valueOf(LocalDate.now());
            MaintenanceRecord record = new MaintenanceRecord(
                    vId,
                    "Routine Servicing & Brake Repair",
                    "Full engine tune-up, front brake pad replacement, synthetic oil change",
                    4500.0,
                    startDate,
                    null,
                    "UNDER_MAINTENANCE",
                    "City Central Workshop",
                    "Customer reported slight squeaking during braking"
            );

            boolean recorded = maintenanceService.recordMaintenance(record);
            if (recorded) {
                System.out.println("PASS: Maintenance record saved with ID #" + record.getMaintenanceId());
            } else {
                System.out.println("FAIL: Maintenance record not saved");
            }

            // Step 3: Check Vehicle Status is now MAINTENANCE
            Vehicle updatedVehicle = vehicleService.getVehicleById(vId);
            if ("MAINTENANCE".equalsIgnoreCase(updatedVehicle.getStatus())) {
                System.out.println("PASS: Vehicle status successfully updated to 'MAINTENANCE'");
            } else {
                System.out.println("FAIL: Expected MAINTENANCE, got: " + updatedVehicle.getStatus());
            }

            // Step 4: Check that vehicle is listed in unavailable list
            System.out.println("\n[Test 3] Indicating Vehicles Unavailable Due to Maintenance");
            List<Vehicle> unavailableList = maintenanceService.getUnavailableVehiclesDueToMaintenance();
            boolean foundInUnavailable = unavailableList.stream().anyMatch(v -> v.getVehicleId() == vId);
            if (foundInUnavailable) {
                System.out.println("PASS: Vehicle #" + vId + " correctly indicated as UNAVAILABLE in maintenance list (" +
                        unavailableList.size() + " total unavailable vehicles)");
            } else {
                System.out.println("FAIL: Vehicle #" + vId + " not found in unavailable list");
            }

            // Step 5: Check that vehicle is excluded from available rentals
            List<Vehicle> availableList = vehicleService.getAvailableVehicles();
            boolean foundInAvailable = availableList.stream().anyMatch(v -> v.getVehicleId() == vId);
            if (!foundInAvailable) {
                System.out.println("PASS: Vehicle #" + vId + " correctly excluded from available vehicles");
            } else {
                System.out.println("FAIL: Vehicle #" + vId + " still appears in available vehicles");
            }

            // Step 6: Attempting to rent vehicle while in maintenance must fail
            System.out.println("\n[Test 4] Verifying rental block for vehicle under maintenance");
            try {
                Rental rentAttempt = new Rental(
                        1, vId,
                        Date.valueOf(LocalDate.now().plusDays(1)),
                        Date.valueOf(LocalDate.now().plusDays(2)),
                        1000.0
                );
                rentalService.addRental(rentAttempt);
                System.out.println("FAIL: Rental was erroneously accepted for vehicle under maintenance!");
            } catch (IllegalArgumentException ex) {
                System.out.println("PASS: Rental correctly blocked: " + ex.getMessage());
            }

            // Step 7: Complete Maintenance and verify vehicle status restored to AVAILABLE
            System.out.println("\n[Test 5] Completing Maintenance for Record #" + record.getMaintenanceId());
            Date completionDate = Date.valueOf(LocalDate.now());
            boolean completed = maintenanceService.completeMaintenance(
                    record.getMaintenanceId(),
                    completionDate,
                    4800.0,
                    "Brake overhaul certified, engine running smoothly"
            );

            if (completed) {
                System.out.println("PASS: Maintenance marked COMPLETED");
            } else {
                System.out.println("FAIL: Could not complete maintenance");
            }

            Vehicle restoredVehicle = vehicleService.getVehicleById(vId);
            if ("AVAILABLE".equalsIgnoreCase(restoredVehicle.getStatus())) {
                System.out.println("PASS: Vehicle #" + vId + " status successfully restored to 'AVAILABLE'");
            } else {
                System.out.println("FAIL: Vehicle status is: " + restoredVehicle.getStatus());
            }

            // Step 8: Verify Vehicle condition linking from return
            System.out.println("\n[Test 6] Testing Vehicle Condition Notes retrieval");
            String conditionSummary = maintenanceService.getVehicleConditionSummary(vId);
            System.out.println("PASS: Vehicle #" + vId + " condition summary from returns: " +
                    (conditionSummary.isEmpty() ? "(No past return condition recorded)" : conditionSummary));

            System.out.println("\n==================================================");
            System.out.println("       ALL MAINTENANCE TESTS COMPLETED!          ");
            System.out.println("==================================================");

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
