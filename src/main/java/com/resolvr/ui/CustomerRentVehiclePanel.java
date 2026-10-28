package com.resolvr.ui;

import com.resolvr.extension.model.RentalQueueEntry;
import com.resolvr.extension.service.RentalQueueService;
import com.resolvr.model.Customer;
import com.resolvr.model.Payment;
import com.resolvr.model.Rental;
import com.resolvr.model.Vehicle;
import com.resolvr.service.PaymentService;
import com.resolvr.service.RentalService;
import com.resolvr.service.VehicleService;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class CustomerRentVehiclePanel extends JPanel {

    private final Customer customer;
    private final VehicleService vehicleService = new VehicleService();
    private final RentalService rentalService = new RentalService();
    private final PaymentService paymentService = new PaymentService();
    private final RentalQueueService queueService = new RentalQueueService();

    private final JComboBox<String> categoryFilter = new JComboBox<>(
            new String[]{"ALL", "Car", "SUV", "Van", "Bike", "Other"}
    );

    private final JComboBox<String> availabilityFilter = new JComboBox<>(
            new String[]{"Available Only", "All Vehicles (Available & Rented)"}
    );

    private final DefaultTableModel vehicleTableModel =
            new DefaultTableModel(new String[]{
                    "Vehicle ID", "Registration", "Model",
                    "Category", "Hourly Price (₹/hr)", "Status", "Waitlist Queue"
            }, 0) {
                @Override
                public boolean isCellEditable(int row, int col) {
                    return false;
                }
            };

    private final JTable vehicleTable = new JTable(vehicleTableModel);

    // Booking form controls
    private final JLabel selectedVehicleLabel = new JLabel("None selected (click a vehicle above)");
    private final JTextField startDateField = new JTextField(LocalDate.now().toString());
    private final JTextField endDateField = new JTextField(LocalDate.now().plusDays(1).toString());
    private final JSpinner hoursSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 720, 1));
    private final JLabel estimatedTotalLabel = new JLabel("₹0.00");
    private final JComboBox<String> paymentMethodBox = new JComboBox<>(new String[]{"UPI", "CARD", "CASH"});
    private final JButton bookButton = new JButton("Confirm & Book Vehicle");

    private Vehicle selectedVehicle = null;

    public CustomerRentVehiclePanel(Customer customer) {
        this.customer = customer;

        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // 1. Header banner
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Browse & Rent Vehicles");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        JLabel hint = new JLabel("Select a vehicle to rent immediately, or join the priority waiting list if the vehicle is currently rented.");
        hint.setForeground(Color.GRAY);
        headerPanel.add(title, BorderLayout.NORTH);
        headerPanel.add(hint, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        // 2. Center: Vehicle Browser Table with Filter
        JPanel tableContainer = new JPanel(new BorderLayout(8, 8));
        tableContainer.setBorder(BorderFactory.createTitledBorder("Available & Rented Vehicles Fleet"));

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filterBar.add(new JLabel("Category:"));
        filterBar.add(categoryFilter);
        filterBar.add(new JLabel("Availability:"));
        filterBar.add(availabilityFilter);
        JButton filterBtn = new JButton("Filter");
        JButton refreshBtn = new JButton("Refresh");
        filterBar.add(filterBtn);
        filterBar.add(refreshBtn);
        tableContainer.add(filterBar, BorderLayout.NORTH);

        vehicleTable.setRowHeight(25);
        vehicleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Status column renderer
        vehicleTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
                if (value != null) {
                    String str = value.toString();
                    if ("AVAILABLE".equalsIgnoreCase(str)) {
                        c.setForeground(new Color(39, 174, 96));
                        setText("AVAILABLE (Ready to rent)");
                    } else if ("RENTED".equalsIgnoreCase(str)) {
                        c.setForeground(new Color(211, 84, 0));
                        setText("RENTED (Waitlist open)");
                    } else if ("MAINTENANCE".equalsIgnoreCase(str)) {
                        c.setForeground(new Color(192, 57, 43));
                        setText("MAINTENANCE");
                    }
                }
                return c;
            }
        });

        tableContainer.add(new JScrollPane(vehicleTable), BorderLayout.CENTER);

        // 3. Bottom: Booking Card
        JPanel bookingCard = new JPanel(new BorderLayout(10, 10));
        bookingCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Book Selected Vehicle or Join Queue"),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JPanel formGrid = new JPanel(new GridLayout(0, 2, 10, 8));

        formGrid.add(new JLabel("Selected Vehicle:"));
        selectedVehicleLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        selectedVehicleLabel.setForeground(new Color(29, 42, 65));
        formGrid.add(selectedVehicleLabel);

        formGrid.add(new JLabel("Renting as:"));
        JLabel customerLabel = new JLabel(customer.getName() + " (Licence: " + customer.getLicenseNo() + ")");
        customerLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        formGrid.add(customerLabel);

        formGrid.add(new JLabel("Start Date (YYYY-MM-DD):"));
        formGrid.add(startDateField);

        formGrid.add(new JLabel("End Date (YYYY-MM-DD):"));
        formGrid.add(endDateField);

        formGrid.add(new JLabel("Rental Duration (Hours):"));
        formGrid.add(hoursSpinner);

        formGrid.add(new JLabel("Payment Method:"));
        formGrid.add(paymentMethodBox);

        formGrid.add(new JLabel("Estimated Total Amount:"));
        estimatedTotalLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        estimatedTotalLabel.setForeground(new Color(39, 85, 145));
        formGrid.add(estimatedTotalLabel);

        bookButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        bookButton.setBackground(new Color(39, 85, 145));
        bookButton.setForeground(Color.WHITE);
        bookButton.setFocusPainted(false);

        JButton recalculateButton = new JButton("Recalculate Cost");

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.add(recalculateButton);
        buttonPanel.add(bookButton);

        bookingCard.add(formGrid, BorderLayout.CENTER);
        bookingCard.add(buttonPanel, BorderLayout.SOUTH);

        // Split view
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableContainer, bookingCard);
        splitPane.setResizeWeight(0.55);
        splitPane.setDividerSize(6);
        add(splitPane, BorderLayout.CENTER);

        // Listeners
        filterBtn.addActionListener(e -> filterVehicles());
        refreshBtn.addActionListener(e -> loadAvailableVehicles());
        availabilityFilter.addActionListener(e -> filterVehicles());
        recalculateButton.addActionListener(e -> updateEstimate());
        hoursSpinner.addChangeListener(e -> updateEstimate());

        vehicleTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && vehicleTable.getSelectedRow() >= 0) {
                int row = vehicleTable.getSelectedRow();
                int vehicleId = Integer.parseInt(vehicleTableModel.getValueAt(row, 0).toString());
                try {
                    selectedVehicle = vehicleService.getVehicleById(vehicleId);
                    if (selectedVehicle != null) {
                        selectedVehicleLabel.setText(
                                selectedVehicle.getModel() + " [" + selectedVehicle.getRegistrationNo() +
                                "] (" + selectedVehicle.getCategory() + ") - ₹" +
                                String.format("%.2f", selectedVehicle.getPricePerHour()) + "/hr"
                        );
                        updateEstimate();

                        // Adjust button mode based on vehicle status
                        if ("RENTED".equalsIgnoreCase(selectedVehicle.getStatus())) {
                            bookButton.setText("Join Waiting List / Queue (Vehicle Rented)");
                            bookButton.setBackground(new Color(211, 84, 0));
                        } else {
                            bookButton.setText("Confirm & Book Vehicle");
                            bookButton.setBackground(new Color(39, 85, 145));
                        }
                    }
                } catch (SQLException ex) {
                    error(ex);
                }
            }
        });

        bookButton.addActionListener(e -> handleBookingOrQueue());

        loadAvailableVehicles();
    }

    private void updateEstimate() {
        if (selectedVehicle == null) {
            estimatedTotalLabel.setText("₹0.00");
            return;
        }

        try {
            int hours = ((Number) hoursSpinner.getValue()).intValue();
            if (hours <= 0) hours = 1;
            double total = hours * selectedVehicle.getPricePerHour();
            estimatedTotalLabel.setText(String.format("₹%.2f (%d hrs @ ₹%.2f/hr)",
                    total, hours, selectedVehicle.getPricePerHour()));
        } catch (Exception ex) {
            estimatedTotalLabel.setText("₹0.00");
        }
    }

    public void loadAvailableVehicles() {
        filterVehicles();
    }

    private void filterVehicles() {
        String cat = (String) categoryFilter.getSelectedItem();
        String avail = (String) availabilityFilter.getSelectedItem();

        try {
            List<Vehicle> list;
            if ("ALL".equals(cat)) {
                list = vehicleService.getAllVehicles();
            } else {
                list = vehicleService.searchByCategory(cat);
            }

            if ("Available Only".equals(avail)) {
                list = list.stream()
                        .filter(v -> "AVAILABLE".equalsIgnoreCase(v.getStatus()))
                        .toList();
            }

            display(list);
            selectedVehicle = null;
            selectedVehicleLabel.setText("None selected (click a vehicle above)");
            estimatedTotalLabel.setText("₹0.00");
            bookButton.setText("Confirm & Book Vehicle");
            bookButton.setBackground(new Color(39, 85, 145));
        } catch (SQLException ex) {
            error(ex);
        }
    }

    private void display(List<Vehicle> vehicles) {
        vehicleTableModel.setRowCount(0);
        for (Vehicle v : vehicles) {
            String queueInfo = "0 waiting";
            try {
                int qCount = queueService.getQueueForVehicle(v.getVehicleId()).size();
                queueInfo = qCount > 0 ? (qCount + " waiting in queue") : "No queue";
            } catch (Exception ignored) {}

            vehicleTableModel.addRow(new Object[]{
                    v.getVehicleId(),
                    v.getRegistrationNo(),
                    v.getModel(),
                    v.getCategory(),
                    String.format("%.2f", v.getPricePerHour()),
                    v.getStatus(),
                    queueInfo
            });
        }
    }

    private void handleBookingOrQueue() {
        if (selectedVehicle == null) {
            message("Please select a vehicle from the table first.");
            return;
        }

        if ("MAINTENANCE".equalsIgnoreCase(selectedVehicle.getStatus())) {
            message("Vehicle #" + selectedVehicle.getVehicleId() + " is currently in maintenance / servicing and cannot be booked or queued.");
            return;
        }

        if ("RENTED".equalsIgnoreCase(selectedVehicle.getStatus())) {
            joinWaitlist();
        } else {
            bookRental();
        }
    }

    private void joinWaitlist() {
        try {
            Date start = Date.valueOf(startDateField.getText().trim());
            Date end = Date.valueOf(endDateField.getText().trim());

            if (end.before(start)) {
                message("End date cannot be earlier than start date.");
                return;
            }

            int hours = ((Number) hoursSpinner.getValue()).intValue();
            if (hours <= 0) {
                message("Duration must be at least 1 hour.");
                return;
            }

            double totalAmount = hours * selectedVehicle.getPricePerHour();
            int currentQueueSize = queueService.getQueueForVehicle(selectedVehicle.getVehicleId()).size();
            int nextPosition = currentQueueSize + 1;

            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Join Waiting List / Priority Queue:\n\n" +
                    "Vehicle: " + selectedVehicle.getModel() + " (" + selectedVehicle.getRegistrationNo() + ")\n" +
                    "Current Status: RENTED by another customer\n" +
                    "Current Queue: " + currentQueueSize + " person(s) waiting ahead\n" +
                    "Your Position: #" + nextPosition + " (First-In-First-Out Priority)\n\n" +
                    "If the current renter cancels their booking, priority will automatically go to you,\n" +
                    "and the vehicle will be immediately booked under your account!\n\n" +
                    "Requested Duration: " + hours + " hour(s)\n" +
                    "Estimated Cost: ₹" + String.format("%.2f", totalAmount) + "\n" +
                    "Dates: " + start + " to " + end + "\n\n" +
                    "Do you want to join the waiting list?",
                    "Join Vehicle Waiting List",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );

            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }

            RentalQueueEntry entry = new RentalQueueEntry(
                    selectedVehicle.getVehicleId(),
                    customer.getCustomerId(),
                    start,
                    end,
                    hours,
                    totalAmount,
                    "Waitlist request via customer portal"
            );

            if (queueService.joinQueue(entry)) {
                JOptionPane.showMessageDialog(
                        this,
                        "Successfully added to Waiting List!\n\n" +
                        "Vehicle: " + selectedVehicle.getModel() + "\n" +
                        "Your Priority Position: #" + entry.getQueuePosition() + "\n" +
                        "Status: WAITING\n\n" +
                        "If the current customer cancels, you will automatically take the vehicle.\n" +
                        "Track your status anytime in the 'My Queue / Waitlist' tab.",
                        "Waitlist Confirmed",
                        JOptionPane.INFORMATION_MESSAGE
                );
                filterVehicles();
            } else {
                message("Could not join waiting list. Please try again.");
            }

        } catch (IllegalArgumentException ex) {
            message("Error: " + ex.getMessage());
        } catch (SQLException ex) {
            error(ex);
        }
    }

    private void bookRental() {
        try {
            Date start = Date.valueOf(startDateField.getText().trim());
            Date end = Date.valueOf(endDateField.getText().trim());

            if (end.before(start)) {
                message("End date cannot be earlier than start date.");
                return;
            }

            int hours = ((Number) hoursSpinner.getValue()).intValue();
            if (hours <= 0) {
                message("Duration must be at least 1 hour.");
                return;
            }

            double totalAmount = hours * selectedVehicle.getPricePerHour();

            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Confirm Booking Details:\n\n" +
                    "Vehicle: " + selectedVehicle.getModel() + " (" + selectedVehicle.getRegistrationNo() + ")\n" +
                    "Duration: " + hours + " hour(s)\n" +
                    "Rate: ₹" + String.format("%.2f", selectedVehicle.getPricePerHour()) + "/hr\n" +
                    "Total Amount: ₹" + String.format("%.2f", totalAmount) + "\n" +
                    "Start Date: " + start + "\n" +
                    "End Date: " + end + "\n\n" +
                    "Do you want to proceed?",
                    "Confirm Rental",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );

            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }

            Rental rental = new Rental(
                    customer.getCustomerId(),
                    selectedVehicle.getVehicleId(),
                    start,
                    end,
                    totalAmount
            );

            if (rentalService.addRental(rental)) {
                // Auto create initial payment record
                String method = (String) paymentMethodBox.getSelectedItem();
                Payment payment = new Payment(
                        rental.getRentalId(),
                        totalAmount,
                        method != null ? method : "UPI",
                        "PENDING"
                );
                paymentService.addPayment(payment);

                JOptionPane.showMessageDialog(
                        this,
                        "Rental booked successfully!\n\n" +
                        "Rental ID: #" + rental.getRentalId() + "\n" +
                        "Total Cost: ₹" + String.format("%.2f", totalAmount) + "\n" +
                        "Status: BOOKED\n\n" +
                        "You can review your booking in 'My Bookings' and complete payment in 'My Payments'.",
                        "Booking Confirmed",
                        JOptionPane.INFORMATION_MESSAGE
                );

                filterVehicles();
            } else {
                message("Could not create rental booking. Please try again.");
            }

        } catch (IllegalArgumentException ex) {
            message("Invalid date format. Please use YYYY-MM-DD.\n" + ex.getMessage());
        } catch (SQLException ex) {
            error(ex);
        }
    }

    private void message(String text) {
        JOptionPane.showMessageDialog(this, text);
    }

    private void error(Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
    }
}
