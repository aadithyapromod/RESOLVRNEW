package com.resolvr.ui;

import com.resolvr.model.Customer;
import com.resolvr.model.Rental;
import com.resolvr.model.Vehicle;
import com.resolvr.model.VehicleReturn;
import com.resolvr.service.RentalService;
import com.resolvr.service.ReturnService;
import com.resolvr.service.VehicleService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class CustomerBookingsPanel extends JPanel {

    private final Customer customer;
    private final RentalService rentalService = new RentalService();
    private final VehicleService vehicleService = new VehicleService();
    private final ReturnService returnService = new ReturnService();

    private final DefaultTableModel tableModel =
            new DefaultTableModel(new String[]{
                    "Rental ID", "Vehicle ID", "Vehicle Model",
                    "Start Date", "End Date", "Total Amount (₹)", "Status"
            }, 0) {
                @Override
                public boolean isCellEditable(int row, int col) {
                    return false;
                }
            };

    private final JTable table = new JTable(tableModel);

    public CustomerBookingsPanel(Customer customer) {
        this.customer = customer;

        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel title = new JLabel("My Bookings");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        JLabel subtitle = new JLabel("View your current and previous vehicle rentals.");
        subtitle.setForeground(Color.GRAY);
        headerPanel.add(title, BorderLayout.NORTH);
        headerPanel.add(subtitle, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        table.setRowHeight(25);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        JButton refreshBtn = new JButton("Refresh Bookings");
        JButton cancelBtn = new JButton("Cancel Selected Booking");
        JButton returnBtn = new JButton("Return This Vehicle");
        returnBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        returnBtn.setBackground(new Color(39, 85, 145));
        returnBtn.setForeground(Color.WHITE);
        returnBtn.setFocusPainted(false);

        JButton detailsBtn = new JButton("View Rental Details");

        controls.add(refreshBtn);
        controls.add(cancelBtn);
        controls.add(returnBtn);
        controls.add(detailsBtn);

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.add(controls, BorderLayout.NORTH);
        content.add(new JScrollPane(table), BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);

        refreshBtn.addActionListener(e -> loadBookings());
        cancelBtn.addActionListener(e -> cancelBooking());
        returnBtn.addActionListener(e -> returnVehicle());
        detailsBtn.addActionListener(e -> viewDetails());

        loadBookings();
    }

    public void loadBookings() {
        try {
            List<Rental> rentals = rentalService.getRentalsByCustomerId(customer.getCustomerId());
            tableModel.setRowCount(0);

            for (Rental r : rentals) {
                String modelName = "Vehicle #" + r.getVehicleId();
                try {
                    Vehicle v = vehicleService.getVehicleById(r.getVehicleId());
                    if (v != null) {
                        modelName = v.getModel() + " (" + v.getRegistrationNo() + ")";
                    }
                } catch (SQLException ignored) {}

                tableModel.addRow(new Object[]{
                        r.getRentalId(),
                        r.getVehicleId(),
                        modelName,
                        r.getStartDate(),
                        r.getEndDate(),
                        String.format("%.2f", r.getTotalAmount()),
                        r.getStatus()
                });
            }
        } catch (SQLException ex) {
            error(ex);
        }
    }

    private void cancelBooking() {
        int row = table.getSelectedRow();
        if (row < 0) {
            message("Select a booking from the table first.");
            return;
        }

        int rentalId = Integer.parseInt(tableModel.getValueAt(row, 0).toString());
        String currentStatus = tableModel.getValueAt(row, 6).toString();

        if ("CANCELLED".equalsIgnoreCase(currentStatus)) {
            message("This rental is already cancelled.");
            return;
        }

        if ("COMPLETED".equalsIgnoreCase(currentStatus)) {
            message("Cannot cancel a completed rental.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to cancel Rental #" + rentalId + "?",
                "Confirm Cancellation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            Rental promoted = rentalService.cancelRentalWithQueuePromotion(rentalId);
            if (promoted != null) {
                JOptionPane.showMessageDialog(
                        this,
                        "Booking #" + rentalId + " has been cancelled.\n\n" +
                        "Priority Waitlist Update:\n" +
                        "The vehicle has been automatically allocated to the next customer waiting in the queue (Rental #" +
                        promoted.getRentalId() + ")!",
                        "Booking Cancelled & Reallocated",
                        JOptionPane.INFORMATION_MESSAGE
                );
            } else {
                message("Booking #" + rentalId + " has been cancelled.");
            }
            loadBookings();
        } catch (SQLException ex) {
            error(ex);
        }
    }

    private void returnVehicle() {
        int row = table.getSelectedRow();
        if (row < 0) {
            message("Select an active rental to return.");
            return;
        }

        int rentalId = Integer.parseInt(tableModel.getValueAt(row, 0).toString());
        String vehicleInfo = tableModel.getValueAt(row, 2).toString();
        String currentStatus = tableModel.getValueAt(row, 6).toString();

        if ("COMPLETED".equalsIgnoreCase(currentStatus)) {
            message("This vehicle has already been returned and completed.");
            return;
        }

        if ("CANCELLED".equalsIgnoreCase(currentStatus)) {
            message("This rental was cancelled.");
            return;
        }

        String notes = JOptionPane.showInputDialog(
                this,
                "Returning: " + vehicleInfo + " (Rental #" + rentalId + ")\n\n" +
                "Enter condition notes / remarks:",
                "Vehicle returned in good condition"
        );

        if (notes == null) {
            return;
        }

        try {
            VehicleReturn vr = new VehicleReturn(
                    rentalId,
                    Date.valueOf(LocalDate.now()),
                    0.0,
                    0.0,
                    notes.trim().isEmpty() ? "Vehicle returned by customer" : notes.trim()
            );

            if (returnService.processReturn(vr)) {
                JOptionPane.showMessageDialog(
                        this,
                        "Vehicle Return Successful!\n\n" +
                        "Rental #" + rentalId + " is now COMPLETED.\n" +
                        "The vehicle has been returned into the available fleet.",
                        "Return Completed",
                        JOptionPane.INFORMATION_MESSAGE
                );
                loadBookings();
            } else {
                message("Failed to process return. Please try again.");
            }
        } catch (SQLException ex) {
            error(ex);
        }
    }

    private void viewDetails() {
        int row = table.getSelectedRow();
        if (row < 0) {
            message("Select a booking from the table first.");
            return;
        }

        int rentalId = Integer.parseInt(tableModel.getValueAt(row, 0).toString());
        String vehicleInfo = tableModel.getValueAt(row, 2).toString();
        String start = tableModel.getValueAt(row, 3).toString();
        String end = tableModel.getValueAt(row, 4).toString();
        String amount = tableModel.getValueAt(row, 5).toString();
        String status = tableModel.getValueAt(row, 6).toString();

        JOptionPane.showMessageDialog(
                this,
                "Rental Summary:\n\n" +
                "Rental ID: #" + rentalId + "\n" +
                "Customer: " + customer.getName() + " (ID: #" + customer.getCustomerId() + ")\n" +
                "Vehicle: " + vehicleInfo + "\n" +
                "Start Date: " + start + "\n" +
                "End Date: " + end + "\n" +
                "Total Amount: ₹" + amount + "\n" +
                "Booking Status: " + status + "\n",
                "Booking Details",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void message(String text) {
        JOptionPane.showMessageDialog(this, text);
    }

    private void error(Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
    }
}
