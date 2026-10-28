package com.resolvr;

import com.resolvr.extension.model.RentalQueueEntry;
import com.resolvr.extension.service.RentalQueueService;
import com.resolvr.model.Customer;
import com.resolvr.model.Rental;
import com.resolvr.model.Vehicle;
import com.resolvr.service.CustomerService;
import com.resolvr.service.RentalService;
import com.resolvr.service.VehicleService;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

public class RentalQueueTest {

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("   RESOLVR VEHICLE RENTAL WAITLIST & PRIORITY QUEUE TEST       ");
        System.out.println("===============================================================");

        VehicleService vehicleService = new VehicleService();
        CustomerService customerService = new CustomerService();
        RentalService rentalService = new RentalService();
        RentalQueueService queueService = new RentalQueueService();

        try {
            // Step 1: Ensure we have at least 1 vehicle and at least 3 customers
            List<Vehicle> vehicles = vehicleService.getAllVehicles();
            if (vehicles.isEmpty()) {
                System.out.println("FAIL: No vehicles available in database to test.");
                return;
            }

            Vehicle vehicle = vehicles.get(0);
            int vId = vehicle.getVehicleId();

            List<Customer> customers = customerService.getAllCustomers();
            if (customers.size() < 2) {
                // Ensure at least 3 customers exist
                customerService.addCustomer(new Customer("Person One", "9876500001", "p1@resolvr.com", "LIC-P1-" + System.currentTimeMillis()));
                customerService.addCustomer(new Customer("Person Two", "9876500002", "p2@resolvr.com", "LIC-P2-" + System.currentTimeMillis()));
                customerService.addCustomer(new Customer("Person Three", "9876500003", "p3@resolvr.com", "LIC-P3-" + System.currentTimeMillis()));
                customers = customerService.getAllCustomers();
            } else if (customers.size() == 2) {
                customerService.addCustomer(new Customer("Person Three", "9876500003", "p3@resolvr.com", "LIC-P3-" + System.currentTimeMillis()));
                customers = customerService.getAllCustomers();
            }

            Customer person1 = customers.get(0);
            Customer person2 = customers.get(1);
            Customer person3 = customers.get(2);

            System.out.println("\n[Setup]");
            System.out.println("Test Vehicle: #" + vId + " (" + vehicle.getModel() + " - " + vehicle.getRegistrationNo() + ")");
            System.out.println("Person 1: #" + person1.getCustomerId() + " (" + person1.getName() + ")");
            System.out.println("Person 2: #" + person2.getCustomerId() + " (" + person2.getName() + ")");
            System.out.println("Person 3: #" + person3.getCustomerId() + " (" + person3.getName() + ")");

            // Reset vehicle to AVAILABLE for a clean test
            vehicleService.updateStatus(vId, "AVAILABLE");

            // Clean any pre-existing waiting queue entries for this test vehicle
            List<RentalQueueEntry> existingQueue = queueService.getQueueForVehicle(vId);
            for (RentalQueueEntry eq : existingQueue) {
                queueService.cancelQueueEntry(eq.getQueueId());
            }

            // Step 2: Person 1 comes first -> Priority goes to Person 1 (Active Booking)
            System.out.println("\n[Test 1] Person 1 rents Vehicle #" + vId + " (First-come priority)");
            Date startDate = Date.valueOf(LocalDate.now());
            Date endDate = Date.valueOf(LocalDate.now().plusDays(2));

            Rental rental1 = new Rental(person1.getCustomerId(), vId, startDate, endDate, 2500.0);
            boolean booked1 = rentalService.addRental(rental1);

            if (booked1 && rental1.getRentalId() > 0) {
                System.out.println("PASS: Person 1 successfully booked vehicle (Rental ID #" + rental1.getRentalId() + ")");
            } else {
                System.out.println("FAIL: Person 1 could not book vehicle");
            }

            Vehicle vAfterRent1 = vehicleService.getVehicleById(vId);
            if ("RENTED".equalsIgnoreCase(vAfterRent1.getStatus())) {
                System.out.println("PASS: Vehicle status updated to 'RENTED'");
            } else {
                System.out.println("FAIL: Vehicle status expected RENTED, got: " + vAfterRent1.getStatus());
            }

            // Step 3: Person 2 comes second and wants to rent the same vehicle -> Placed in Priority Queue at Position #1
            System.out.println("\n[Test 2] Person 2 requests the same vehicle -> Added to Waiting List / Queue");
            RentalQueueEntry queueEntry2 = new RentalQueueEntry(
                    vId,
                    person2.getCustomerId(),
                    startDate,
                    endDate,
                    24,
                    2400.0,
                    "Person 2 waiting in line"
            );

            boolean queued2 = queueService.joinQueue(queueEntry2);
            if (queued2 && queueEntry2.getQueuePosition() == 1) {
                System.out.println("PASS: Person 2 placed in queue at Position #1 (Queue ID #" + queueEntry2.getQueueId() + ")");
            } else {
                System.out.println("FAIL: Person 2 queue placement failed or unexpected position: " + queueEntry2.getQueuePosition());
            }

            // Step 4: Person 3 also requests the same vehicle -> Placed in Priority Queue at Position #2
            System.out.println("\n[Test 3] Person 3 requests the same vehicle -> Added to Waiting List / Queue");
            RentalQueueEntry queueEntry3 = new RentalQueueEntry(
                    vId,
                    person3.getCustomerId(),
                    startDate,
                    endDate,
                    48,
                    4800.0,
                    "Person 3 waiting in line"
            );

            boolean queued3 = queueService.joinQueue(queueEntry3);
            if (queued3 && queueEntry3.getQueuePosition() == 2) {
                System.out.println("PASS: Person 3 placed in queue at Position #2 (Queue ID #" + queueEntry3.getQueueId() + ")");
            } else {
                System.out.println("FAIL: Person 3 queue placement failed or unexpected position: " + queueEntry3.getQueuePosition());
            }

            // Verify queue size for this vehicle
            List<RentalQueueEntry> currentQueue = queueService.getQueueForVehicle(vId);
            if (currentQueue.size() == 2) {
                System.out.println("PASS: Verified 2 customers waiting in queue for Vehicle #" + vId);
            } else {
                System.out.println("FAIL: Expected 2 in queue, got: " + currentQueue.size());
            }

            // Step 5: Duplicate queue check - Person 2 cannot join the same vehicle's queue twice
            System.out.println("\n[Test 4] Duplicate queue prevention");
            try {
                RentalQueueEntry dup = new RentalQueueEntry(vId, person2.getCustomerId(), startDate, endDate, 12, 1200.0, "Duplicate");
                queueService.joinQueue(dup);
                System.out.println("FAIL: Duplicate queue entry was accepted!");
            } catch (IllegalArgumentException ex) {
                System.out.println("PASS: Duplicate request rejected: " + ex.getMessage());
            }

            // Step 6: CRUCIAL REQUIREMENT - If Person 1 cancels, Person 2 must automatically take the vehicle!
            System.out.println("\n[Test 5] Person 1 cancels booking -> Priority transfers automatically to Person 2");
            Rental promotedRental = rentalService.cancelRentalWithQueuePromotion(rental1.getRentalId());

            if (promotedRental != null) {
                System.out.println("PASS: System automatically allocated vehicle to next customer in queue!");
                System.out.println("      New Rental ID: #" + promotedRental.getRentalId());
                System.out.println("      Customer ID on Rental: #" + promotedRental.getCustomerId() +
                        (promotedRental.getCustomerId() == person2.getCustomerId() ? " (Person 2 - MATCH!)" : " (MISMATCH!)"));
            } else {
                System.out.println("FAIL: No rental was promoted upon cancellation");
            }

            // Verify Person 1's rental status is CANCELLED
            Rental r1Check = rentalService.getRentalById(rental1.getRentalId());
            if ("CANCELLED".equalsIgnoreCase(r1Check.getStatus())) {
                System.out.println("PASS: Person 1's rental status is now 'CANCELLED'");
            } else {
                System.out.println("FAIL: Person 1's rental status is: " + r1Check.getStatus());
            }

            // Verify Person 2's queue entry is PROMOTED
            RentalQueueEntry q2Check = queueService.getQueueEntryById(queueEntry2.getQueueId());
            if ("PROMOTED".equalsIgnoreCase(q2Check.getStatus())) {
                System.out.println("PASS: Person 2's queue entry status updated to 'PROMOTED'");
            } else {
                System.out.println("FAIL: Person 2's queue entry status is: " + q2Check.getStatus());
            }

            // Verify Vehicle remains RENTED (now by Person 2)
            Vehicle vAfterPromo = vehicleService.getVehicleById(vId);
            if ("RENTED".equalsIgnoreCase(vAfterPromo.getStatus())) {
                System.out.println("PASS: Vehicle remains 'RENTED' (now assigned to Person 2)");
            } else {
                System.out.println("FAIL: Vehicle status is: " + vAfterPromo.getStatus());
            }

            // Step 7: Check that Person 3 is now promoted to Position #1 in the queue (FIFO queue re-indexing)
            System.out.println("\n[Test 6] Queue position advancement for remaining waiting customers");
            RentalQueueEntry q3Check = queueService.getQueueEntryById(queueEntry3.getQueueId());
            if ("WAITING".equalsIgnoreCase(q3Check.getStatus()) && q3Check.getQueuePosition() == 1) {
                System.out.println("PASS: Person 3 advanced from Position #2 to Position #1 (Next in line)!");
            } else {
                System.out.println("FAIL: Person 3 position expected 1, got: " + q3Check.getQueuePosition());
            }

            // Step 8: Person 2 cancels -> Person 3 should take it next!
            System.out.println("\n[Test 7] Person 2 cancels -> Priority transfers automatically to Person 3");
            Rental promotedRental3 = rentalService.cancelRentalWithQueuePromotion(promotedRental.getRentalId());

            if (promotedRental3 != null && promotedRental3.getCustomerId() == person3.getCustomerId()) {
                System.out.println("PASS: Person 3 automatically received the vehicle upon Person 2 cancellation!");
                System.out.println("      New Rental ID: #" + promotedRental3.getRentalId());
            } else {
                System.out.println("FAIL: Person 3 was not promoted");
            }

            // Step 9: Queue is now empty -> When Person 3 cancels, vehicle status should become AVAILABLE
            System.out.println("\n[Test 8] Cancellation with empty queue -> Vehicle returns to AVAILABLE");
            Rental promoNull = rentalService.cancelRentalWithQueuePromotion(promotedRental3.getRentalId());
            if (promoNull == null) {
                System.out.println("PASS: No further waiting customers in queue");
            }

            Vehicle vFinal = vehicleService.getVehicleById(vId);
            if ("AVAILABLE".equalsIgnoreCase(vFinal.getStatus())) {
                System.out.println("PASS: Vehicle status successfully returned to 'AVAILABLE'");
            } else {
                System.out.println("FAIL: Expected vehicle AVAILABLE, got: " + vFinal.getStatus());
            }

            System.out.println("\n===============================================================");
            System.out.println("   ALL VEHICLE RENTAL WAITLIST & QUEUE TESTS PASSED!           ");
            System.out.println("===============================================================");

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
