package com.resolvr.extension.service;

import com.resolvr.dao.CustomerDAO;
import com.resolvr.dao.PaymentDAO;
import com.resolvr.dao.RentalDAO;
import com.resolvr.dao.VehicleDAO;
import com.resolvr.extension.dao.RentalQueueDAO;
import com.resolvr.extension.model.RentalQueueEntry;
import com.resolvr.model.Customer;
import com.resolvr.model.Payment;
import com.resolvr.model.Rental;
import com.resolvr.model.Vehicle;

import java.sql.SQLException;
import java.util.List;

public class RentalQueueService {

    private final RentalQueueDAO queueDAO;
    private final VehicleDAO vehicleDAO;
    private final CustomerDAO customerDAO;
    private final RentalDAO rentalDAO;
    private final PaymentDAO paymentDAO;

    public RentalQueueService() {
        this.queueDAO = new RentalQueueDAO();
        this.vehicleDAO = new VehicleDAO();
        this.customerDAO = new CustomerDAO();
        this.rentalDAO = new RentalDAO();
        this.paymentDAO = new PaymentDAO();
    }

    public RentalQueueService(RentalQueueDAO queueDAO, VehicleDAO vehicleDAO,
                              CustomerDAO customerDAO, RentalDAO rentalDAO, PaymentDAO paymentDAO) {
        this.queueDAO = queueDAO;
        this.vehicleDAO = vehicleDAO;
        this.customerDAO = customerDAO;
        this.rentalDAO = rentalDAO;
        this.paymentDAO = paymentDAO;
    }

    // Add a customer to the waiting list / priority queue for a vehicle
    public boolean joinQueue(RentalQueueEntry entry) throws SQLException {
        if (entry == null) {
            throw new IllegalArgumentException("Queue entry cannot be null.");
        }

        if (entry.getCustomerId() <= 0) {
            throw new IllegalArgumentException("A valid customer ID is required.");
        }

        Customer customer = customerDAO.getCustomerById(entry.getCustomerId());
        if (customer == null) {
            throw new IllegalArgumentException("Customer #" + entry.getCustomerId() + " does not exist.");
        }

        if (entry.getVehicleId() <= 0) {
            throw new IllegalArgumentException("A valid vehicle ID is required.");
        }

        Vehicle vehicle = vehicleDAO.getVehicleById(entry.getVehicleId());
        if (vehicle == null) {
            throw new IllegalArgumentException("Vehicle #" + entry.getVehicleId() + " does not exist.");
        }

        if ("MAINTENANCE".equalsIgnoreCase(vehicle.getStatus())) {
            throw new IllegalArgumentException("Cannot join queue: Vehicle #" + entry.getVehicleId() + " is under maintenance / servicing.");
        }

        if (entry.getStartDate() == null || entry.getEndDate() == null) {
            throw new IllegalArgumentException("Start date and end date are required.");
        }

        if (entry.getEndDate().before(entry.getStartDate())) {
            throw new IllegalArgumentException("End date cannot be earlier than start date.");
        }

        if (entry.getDurationHours() <= 0) {
            entry.setDurationHours(4);
        }

        if (queueDAO.isCustomerInQueue(entry.getVehicleId(), entry.getCustomerId())) {
            throw new IllegalArgumentException(
                    "Customer (" + customer.getName() + ") is already in the waiting list for this vehicle."
            );
        }

        if (entry.getEstimatedAmount() <= 0) {
            double amount = entry.getDurationHours() * vehicle.getPricePerHour();
            entry.setEstimatedAmount(amount);
        }

        entry.setStatus("WAITING");

        return queueDAO.addToQueue(entry);
    }

    // Cancel an entry in the waiting list and re-index positions of remaining entries
    public boolean cancelQueueEntry(int queueId) throws SQLException {
        if (queueId <= 0) {
            throw new IllegalArgumentException("Invalid queue ID.");
        }

        RentalQueueEntry entry = queueDAO.getQueueEntryById(queueId);
        if (entry == null) {
            throw new IllegalArgumentException("Queue entry #" + queueId + " not found.");
        }

        if ("CANCELLED".equalsIgnoreCase(entry.getStatus())) {
            throw new IllegalArgumentException("Queue entry is already cancelled.");
        }

        if ("PROMOTED".equalsIgnoreCase(entry.getStatus())) {
            throw new IllegalArgumentException("Cannot cancel an entry that has already been promoted to a booking.");
        }

        boolean updated = queueDAO.updateQueueStatus(queueId, "CANCELLED");
        if (updated) {
            queueDAO.recalculatePositions(entry.getVehicleId());
        }
        return updated;
    }

    // Promote the next customer in the queue to an active booking (Person 2 takes vehicle)
    public Rental promoteNextInQueue(int vehicleId) throws SQLException {
        if (vehicleId <= 0) {
            throw new IllegalArgumentException("Invalid vehicle ID.");
        }

        RentalQueueEntry nextInLine = queueDAO.getFirstWaitingInQueue(vehicleId);
        if (nextInLine == null) {
            return null; // No one in the queue
        }

        // Create the rental for the next person in line
        Rental newRental = new Rental(
                nextInLine.getCustomerId(),
                vehicleId,
                nextInLine.getStartDate(),
                nextInLine.getEndDate(),
                nextInLine.getEstimatedAmount()
        );
        newRental.setStatus("BOOKED");

        boolean created = rentalDAO.addRental(newRental);
        if (!created) {
            throw new SQLException("Failed to create promoted rental for customer #" + nextInLine.getCustomerId());
        }

        // Vehicle is now booked/rented by this next person
        vehicleDAO.updateStatus(vehicleId, "RENTED");

        // Automatically create initial pending payment record
        Payment payment = new Payment(
                newRental.getRentalId(),
                newRental.getTotalAmount(),
                "PENDING",
                "PENDING"
        );
        paymentDAO.addPayment(payment);

        // Update queue status of the promoted person
        queueDAO.updateQueueStatus(nextInLine.getQueueId(), "PROMOTED");

        // Recalculate positions for remaining waiting customers (Position 2 becomes 1, etc.)
        queueDAO.recalculatePositions(vehicleId);

        return newRental;
    }

    // Handle rental cancellation: If Person 1 cancels, automatically promote Person 2 from queue!
    // If no one is in queue, vehicle returns to AVAILABLE.
    public Rental handleRentalCancellation(int vehicleId) throws SQLException {
        RentalQueueEntry next = queueDAO.getFirstWaitingInQueue(vehicleId);
        if (next != null) {
            // Person 2 in queue takes the vehicle!
            return promoteNextInQueue(vehicleId);
        } else {
            // No one in queue -> vehicle is now AVAILABLE
            vehicleDAO.updateStatus(vehicleId, "AVAILABLE");
            return null;
        }
    }

    public List<RentalQueueEntry> getAllQueueEntries() throws SQLException {
        return queueDAO.getAllQueueEntries();
    }

    public List<RentalQueueEntry> getQueueForVehicle(int vehicleId) throws SQLException {
        if (vehicleId <= 0) {
            throw new IllegalArgumentException("Invalid vehicle ID.");
        }
        return queueDAO.getQueueForVehicle(vehicleId);
    }

    public List<RentalQueueEntry> getQueueByCustomerId(int customerId) throws SQLException {
        if (customerId <= 0) {
            throw new IllegalArgumentException("Invalid customer ID.");
        }
        return queueDAO.getQueueByCustomerId(customerId);
    }

    public RentalQueueEntry getQueueEntryById(int queueId) throws SQLException {
        if (queueId <= 0) {
            throw new IllegalArgumentException("Invalid queue ID.");
        }
        return queueDAO.getQueueEntryById(queueId);
    }

    public int getActiveQueueCount() throws SQLException {
        return queueDAO.getActiveQueueCount();
    }

    public int getActiveVehiclesInQueueCount() throws SQLException {
        return queueDAO.getActiveVehiclesInQueueCount();
    }
}
