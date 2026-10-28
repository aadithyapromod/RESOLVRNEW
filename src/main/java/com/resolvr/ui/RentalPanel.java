package com.resolvr.ui;

import com.resolvr.model.Rental;
import com.resolvr.service.RentalService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

public class RentalPanel extends JPanel {
    private final RentalService service = new RentalService();

    private final JTextField customerId = new JTextField();
    private final JTextField vehicleId = new JTextField();
    private final JTextField startDate = new JTextField();
    private final JTextField endDate = new JTextField();

    private final JComboBox<String> status = new JComboBox<>(
            new String[]{"BOOKED", "ACTIVE", "COMPLETED", "CANCELLED"});

    private final DefaultTableModel tableModel =
            new DefaultTableModel(new String[]{
                    "Rental ID", "Customer ID", "Vehicle ID",
                    "Start Date", "End Date", "Total Amount", "Status"
            }, 0) {
                @Override
                public boolean isCellEditable(int row, int col) {
                    return false;
                }
            };

    private final JTable table = new JTable(tableModel);

    public RentalPanel() {
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Rental Management");
        title.setFont(new Font("SansSerif", Font.BOLD, 23));
        add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.setBorder(BorderFactory.createTitledBorder("Create Rental"));

        form.add(new JLabel("Customer ID"));
        form.add(customerId);
        form.add(new JLabel("Vehicle ID"));
        form.add(vehicleId);
        form.add(new JLabel("Start date (YYYY-MM-DD)"));
        form.add(startDate);
        form.add(new JLabel("End date (YYYY-MM-DD)"));
        form.add(endDate);

        JButton create = new JButton("Create Rental");
        JButton refresh = new JButton("Refresh");
        JButton update = new JButton("Update Selected Status");

        form.add(create);
        form.add(refresh);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(new JLabel("New status:"));
        controls.add(status);
        controls.add(update);

        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.add(form, BorderLayout.CENTER);
        top.add(controls, BorderLayout.SOUTH);

        table.setRowHeight(25);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.add(top, BorderLayout.NORTH);
        content.add(new JScrollPane(table), BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);

        create.addActionListener(e -> createRental());
        refresh.addActionListener(e -> loadRentals());
        update.addActionListener(e -> updateStatus());

        loadRentals();
    }

    private void createRental() {
        try {
            int customer = Integer.parseInt(customerId.getText().trim());
            int vehicle = Integer.parseInt(vehicleId.getText().trim());

            Date start = Date.valueOf(startDate.getText().trim());
            Date end = Date.valueOf(endDate.getText().trim());

            Rental rental = new Rental(customer, vehicle, start, end, 0);

            if (service.addRental(rental)) {
                message("Rental created. Total amount: ₹"
                        + rental.getTotalAmount());
                loadRentals();
            } else {
                message("Rental could not be created.");
            }
        } catch (NumberFormatException ex) {
            message("Customer ID and Vehicle ID must be whole numbers.");
        } catch (IllegalArgumentException ex) {
            message("Check the IDs and dates. Use YYYY-MM-DD.\n"
                    + ex.getMessage());
        } catch (SQLException ex) {
            error(ex);
        }
    }

    private void loadRentals() {
        try {
            List<Rental> rentals = service.getAllRentals();
            tableModel.setRowCount(0);

            for (Rental r : rentals) {
                tableModel.addRow(new Object[]{
                        r.getRentalId(), r.getCustomerId(),
                        r.getVehicleId(), r.getStartDate(),
                        r.getEndDate(), r.getTotalAmount(), r.getStatus()
                });
            }
        } catch (SQLException ex) {
            error(ex);
        }
    }

    private void updateStatus() {
        int row = table.getSelectedRow();
        if (row < 0) {
            message("Select a rental from the table first.");
            return;
        }

        int id = Integer.parseInt(
                tableModel.getValueAt(row, 0).toString());
        String newStatus = (String) status.getSelectedItem();

        try {
            if ("CANCELLED".equalsIgnoreCase(newStatus)) {
                Rental promoted = service.cancelRentalWithQueuePromotion(id);
                if (promoted != null) {
                    message("Rental #" + id + " cancelled.\n\n" +
                            "Vehicle automatically allocated to the next customer in the waitlist queue (New Rental #" +
                            promoted.getRentalId() + ")!");
                } else {
                    message("Rental #" + id + " cancelled. Vehicle is now AVAILABLE.");
                }
            } else if (service.updateRentalStatus(id, newStatus)) {
                message("Rental status updated to " + newStatus + ".");
            } else {
                message("Rental status was not updated.");
            }
            loadRentals();
        } catch (SQLException | IllegalArgumentException ex) {
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
