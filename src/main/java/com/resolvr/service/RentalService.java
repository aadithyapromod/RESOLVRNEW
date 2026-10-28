package com.resolvr.service;

import com.resolvr.dao.CustomerDAO;
import com.resolvr.dao.RentalDAO;
import com.resolvr.dao.VehicleDAO;
import com.resolvr.extension.service.RentalQueueService;
import com.resolvr.model.Customer;
import com.resolvr.model.Rental;
import com.resolvr.model.Vehicle;

import java.sql.SQLException;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class RentalService {

    private final RentalDAO rentalDAO;
    private final VehicleDAO vehicleDAO;
    private final CustomerDAO customerDAO;

    public RentalService() {
        this.rentalDAO = new RentalDAO();
        this.vehicleDAO = new VehicleDAO();
        this.customerDAO = new CustomerDAO();
    }

    // Create a rental after validating the booking
    public boolean addRental(Rental rental) throws SQLException {

        if (rental == null) {
            throw new IllegalArgumentException(
                    "Rental cannot be null."
            );
        }

        if (rental.getCustomerId() <= 0
                || rental.getVehicleId() <= 0) {
            throw new IllegalArgumentException(
                    "Valid customer and vehicle IDs are required."
            );
        }

        if (rental.getStartDate() == null
                || rental.getEndDate() == null) {
            throw new IllegalArgumentException(
                    "Start date and end date are required."
            );
        }

        if (rental.getEndDate().before(rental.getStartDate())) {
            throw new IllegalArgumentException(
                    "End date cannot be before the start date."
            );
        }

        // Verify that the customer exists
        Customer customer =
                customerDAO.getCustomerById(rental.getCustomerId());

        if (customer == null) {
            throw new IllegalArgumentException(
                    "Customer does not exist."
            );
        }

        // Find the requested vehicle
        Vehicle selectedVehicle = vehicleDAO.getVehicleById(rental.getVehicleId());

        if (selectedVehicle == null) {
            throw new IllegalArgumentException(
                    "Vehicle does not exist."
            );
        }

        if (!"AVAILABLE".equalsIgnoreCase(selectedVehicle.getStatus())) {
            if ("MAINTENANCE".equalsIgnoreCase(selectedVehicle.getStatus())) {
                throw new IllegalArgumentException(
                        "Vehicle #" + selectedVehicle.getVehicleId() + " (" + selectedVehicle.getModel()
                        + ") is currently unavailable due to maintenance / servicing."
                );
            }
            throw new IllegalArgumentException(
                    "Vehicle is not currently available (" + selectedVehicle.getStatus() + ")."
            );
        }

        // Check for overlapping bookings
        List<Rental> existingRentals = rentalDAO.getAllRentals();

        for (Rental existing : existingRentals) {

            if (existing.getVehicleId() != rental.getVehicleId()) {
                continue;
            }

            String status = existing.getStatus();

            if (status != null &&
                    (status.equalsIgnoreCase("CANCELLED")
                    || status.equalsIgnoreCase("COMPLETED")
                    || status.equalsIgnoreCase("RETURNED"))) {
                continue;
            }

            boolean overlaps =
                    !rental.getEndDate().before(existing.getStartDate())
                    && !rental.getStartDate().after(existing.getEndDate());

            if (overlaps) {
                throw new IllegalArgumentException(
                        "This vehicle already has a booking "
                        + "during the selected dates."
                );
            }
        }

        // Calculate the rental cost if not already specified
        if (rental.getTotalAmount() <= 0) {
            long days = ChronoUnit.DAYS.between(
                    rental.getStartDate().toLocalDate(),
                    rental.getEndDate().toLocalDate()
            );

            long hours = days <= 0 ? 1 : days * 24;
            double totalAmount = hours * selectedVehicle.getPricePerHour();
            rental.setTotalAmount(totalAmount);
        }

        rental.setStatus("BOOKED");

        boolean success = rentalDAO.addRental(rental);
        if (success) {
            // Update vehicle status so it reflects as rented
            vehicleDAO.updateStatus(selectedVehicle.getVehicleId(), "RENTED");
        }
        return success;
    }

    // Retrieve all rentals
    public List<Rental> getAllRentals() throws SQLException {
        return rentalDAO.getAllRentals();
    }

    // Retrieve one rental by ID
    public Rental getRentalById(int rentalId) throws SQLException {

        if (rentalId <= 0) {
            throw new IllegalArgumentException(
                    "Invalid rental ID."
            );
        }

        return rentalDAO.getRentalById(rentalId);
    }

    // Retrieve rentals for a customer
    public List<Rental> getRentalsByCustomerId(int customerId) throws SQLException {
        if (customerId <= 0) {
            throw new IllegalArgumentException(
                    "Invalid customer ID."
            );
        }
        return rentalDAO.getRentalsByCustomerId(customerId);
    }

    // Update rental status
    public boolean updateRentalStatus(int rentalId, String status)
            throws SQLException {

        if (rentalId <= 0) {
            throw new IllegalArgumentException(
                    "Invalid rental ID."
            );
        }

        if (status == null ||
                !(status.equalsIgnoreCase("BOOKED")
                || status.equalsIgnoreCase("ACTIVE")
                || status.equalsIgnoreCase("COMPLETED")
                || status.equalsIgnoreCase("CANCELLED"))) {
            throw new IllegalArgumentException(
                    "Invalid rental status."
            );
        }

        boolean success = rentalDAO.updateRentalStatus(
                rentalId, status.toUpperCase()
        );

        if (success) {
            Rental r = rentalDAO.getRentalById(rentalId);
            if (r != null) {
                if (status.equalsIgnoreCase("CANCELLED")) {
                    // Check if there is someone in the priority queue waiting for this vehicle.
                    // If yes: promote Person 2 so Person 2 takes the vehicle!
                    // If no: vehicle status becomes AVAILABLE.
                    try {
                        RentalQueueService queueService = new RentalQueueService();
                        Rental promoted = queueService.handleRentalCancellation(r.getVehicleId());
                        if (promoted == null) {
                            vehicleDAO.updateStatus(r.getVehicleId(), "AVAILABLE");
                        }
                    } catch (Exception ex) {
                        System.err.println("Note: could not process queue promotion on cancellation: " + ex.getMessage());
                        vehicleDAO.updateStatus(r.getVehicleId(), "AVAILABLE");
                    }
                } else if (status.equalsIgnoreCase("COMPLETED")) {
                    vehicleDAO.updateStatus(r.getVehicleId(), "AVAILABLE");
                }
            }
        }

        return success;
    }

    // Cancel rental and return newly promoted rental if a customer was waiting in queue
    public Rental cancelRentalWithQueuePromotion(int rentalId) throws SQLException {
        if (rentalId <= 0) {
            throw new IllegalArgumentException("Invalid rental ID.");
        }

        Rental r = rentalDAO.getRentalById(rentalId);
        if (r == null) {
            throw new IllegalArgumentException("Rental #" + rentalId + " does not exist.");
        }

        boolean success = rentalDAO.updateRentalStatus(rentalId, "CANCELLED");
        if (!success) {
            return null;
        }

        RentalQueueService queueService = new RentalQueueService();
        Rental promoted = queueService.handleRentalCancellation(r.getVehicleId());
        if (promoted == null) {
            vehicleDAO.updateStatus(r.getVehicleId(), "AVAILABLE");
        }
        return promoted;
    }
}
