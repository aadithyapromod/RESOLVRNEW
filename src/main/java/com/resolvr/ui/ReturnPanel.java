package com.resolvr.ui;

import com.resolvr.model.Rental;
import com.resolvr.model.VehicleReturn;
import com.resolvr.service.RentalService;
import com.resolvr.service.ReturnService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class ReturnPanel extends JPanel {

    private final ReturnService returnService = new ReturnService();
    private final RentalService rentalService = new RentalService();

    private final JTextField rentalId = new JTextField();
    private final JTextField returnDate = new JTextField(LocalDate.now().toString());
    private final JTextField damageCharge = new JTextField("0.00");
    private final JTextField refundAmount = new JTextField("0.00");
    private final JTextField conditionNotes = new JTextField("Vehicle returned in good condition");

    private final DefaultTableModel tableModel =
            new DefaultTableModel(new String[]{
                    "Return ID", "Rental ID", "Actual Return Date",
                    "Damage Charge (₹)", "Refund Amount (₹)", "Condition / Notes"
            }, 0) {
                @Override
                public boolean isCellEditable(int row, int col) {
                    return false;
                }
            };

    private final JTable table = new JTable(tableModel);

    public ReturnPanel() {
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Vehicle Return Management");
        title.setFont(new Font("SansSerif", Font.BOLD, 23));
        add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.setBorder(BorderFactory.createTitledBorder("Process Vehicle Return"));

        form.add(new JLabel("Rental ID"));
        form.add(rentalId);

        form.add(new JLabel("Actual Return Date (YYYY-MM-DD)"));
        form.add(returnDate);

        form.add(new JLabel("Damage Charge (₹)"));
        form.add(damageCharge);

        form.add(new JLabel("Refund Amount (₹)"));
        form.add(refundAmount);

        form.add(new JLabel("Condition Notes / Remarks"));
        form.add(conditionNotes);

        JButton process = new JButton("Process Return");
        process.setFont(new Font("SansSerif", Font.BOLD, 13));
        process.setBackground(new Color(39, 85, 145));
        process.setForeground(Color.WHITE);
        process.setFocusPainted(false);

        JButton refresh = new JButton("Refresh");
        JButton search = new JButton("Search by Rental ID");
        JButton clear = new JButton("Clear Form");

        form.add(process);
        form.add(refresh);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        controls.add(search);
        controls.add(clear);

        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.add(form, BorderLayout.CENTER);
        top.add(controls, BorderLayout.SOUTH);

        table.setRowHeight(25);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.add(top, BorderLayout.NORTH);
        content.add(new JScrollPane(table), BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);

        process.addActionListener(e -> processReturn());
        refresh.addActionListener(e -> loadReturns());
        search.addActionListener(e -> searchByRentalId());
        clear.addActionListener(e -> clearForm());

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                int row = table.getSelectedRow();
                rentalId.setText(cell(row, 1));
                returnDate.setText(cell(row, 2));
                damageCharge.setText(cell(row, 3));
                refundAmount.setText(cell(row, 4));
                conditionNotes.setText(cell(row, 5));
            }
        });

        loadReturns();
    }

    private String cell(int row, int col) {
        Object val = tableModel.getValueAt(row, col);
        return val == null ? "" : val.toString();
    }

    private void clearForm() {
        rentalId.setText("");
        returnDate.setText(LocalDate.now().toString());
        damageCharge.setText("0.00");
        refundAmount.setText("0.00");
        conditionNotes.setText("Vehicle returned in good condition");
        table.clearSelection();
    }

    private void processReturn() {
        String rentalIdText = rentalId.getText().trim();
        String dateText = returnDate.getText().trim();
        String damageText = damageCharge.getText().trim();
        String refundText = refundAmount.getText().trim();
        String notes = conditionNotes.getText().trim();

        if (rentalIdText.isEmpty() || dateText.isEmpty()) {
            message("Rental ID and Return Date are required.");
            return;
        }

        try {
            int rId = Integer.parseInt(rentalIdText);
            Date actualDate = Date.valueOf(dateText);
            double damage = damageText.isEmpty() ? 0.0 : Double.parseDouble(damageText);
            double refund = refundText.isEmpty() ? 0.0 : Double.parseDouble(refundText);

            if (damage < 0 || refund < 0) {
                message("Damage charge and refund amount cannot be negative.");
                return;
            }

            Rental rental = rentalService.getRentalById(rId);
            if (rental == null) {
                message("Rental #" + rId + " not found.");
                return;
            }

            if ("COMPLETED".equalsIgnoreCase(rental.getStatus())
                    || "CANCELLED".equalsIgnoreCase(rental.getStatus())) {
                message("Rental #" + rId + " is already " + rental.getStatus() + ".");
                return;
            }

            VehicleReturn vr = new VehicleReturn(rId, actualDate, damage, refund, notes);

            if (returnService.processReturn(vr)) {
                boolean damaged = damage > 0 || (notes != null && (
                        notes.toLowerCase().contains("damage") ||
                        notes.toLowerCase().contains("crash") ||
                        notes.toLowerCase().contains("repair") ||
                        notes.toLowerCase().contains("accident") ||
                        notes.toLowerCase().contains("dent") ||
                        notes.toLowerCase().contains("broken")));

                String statusUpdateMsg = damaged
                        ? "Vehicle #" + rental.getVehicleId() + " marked as MAINTENANCE (Unavailable for rentals) due to reported condition/damage.\nInitial repair record automatically logged in Maintenance module."
                        : "Vehicle #" + rental.getVehicleId() + " is now marked AVAILABLE for new rentals.";

                JOptionPane.showMessageDialog(
                        this,
                        "Vehicle Return Processed Successfully!\n\n" +
                        "Rental ID: #" + rId + "\n" +
                        "Return Date: " + actualDate + "\n" +
                        "Damage Charge: ₹" + String.format("%.2f", damage) + "\n" +
                        "Refund Amount: ₹" + String.format("%.2f", refund) + "\n\n" +
                        "Rental status set to COMPLETED.\n" +
                        statusUpdateMsg,
                        "Return Completed",
                        JOptionPane.INFORMATION_MESSAGE
                );
                clearForm();
                loadReturns();
            } else {
                message("Could not record vehicle return. Please try again.");
            }

        } catch (NumberFormatException ex) {
            message("Rental ID, damage charge, and refund must be valid numbers.");
        } catch (IllegalArgumentException ex) {
            message("Validation error: " + ex.getMessage());
        } catch (SQLException ex) {
            error(ex);
        }
    }

    public void loadReturns() {
        try {
            List<VehicleReturn> returns = returnService.getAllReturns();
            display(returns);
        } catch (SQLException ex) {
            error(ex);
        }
    }

    private void searchByRentalId() {
        String input = JOptionPane.showInputDialog(
                this, "Enter Rental ID to search return record:");
        if (input == null || input.trim().isEmpty()) {
            return;
        }

        try {
            int rId = Integer.parseInt(input.trim());
            VehicleReturn vr = returnService.getReturnByRentalId(rId);
            tableModel.setRowCount(0);

            if (vr != null) {
                tableModel.addRow(new Object[]{
                        vr.getReturnId(),
                        vr.getRentalId(),
                        vr.getActualReturnDate(),
                        String.format("%.2f", vr.getDamageCharge()),
                        String.format("%.2f", vr.getRefundAmount()),
                        vr.getConditionNotes()
                });
            } else {
                message("No return record found for Rental #" + rId + ".");
            }
        } catch (NumberFormatException ex) {
            message("Rental ID must be a whole number.");
        } catch (SQLException ex) {
            error(ex);
        }
    }

    private void display(List<VehicleReturn> returns) {
        tableModel.setRowCount(0);
        for (VehicleReturn vr : returns) {
            tableModel.addRow(new Object[]{
                    vr.getReturnId(),
                    vr.getRentalId(),
                    vr.getActualReturnDate(),
                    String.format("%.2f", vr.getDamageCharge()),
                    String.format("%.2f", vr.getRefundAmount()),
                    vr.getConditionNotes()
            });
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
