package com.resolvr.extension.ui;

import com.resolvr.extension.model.MaintenanceRecord;
import com.resolvr.extension.service.MaintenanceService;
import com.resolvr.model.Vehicle;
import com.resolvr.service.VehicleService;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class MaintenancePanel extends JPanel {

    private final MaintenanceService maintenanceService = new MaintenanceService();
    private final VehicleService vehicleService = new VehicleService();

    // Form inputs
    private final JComboBox<VehicleComboItem> vehicleCombo = new JComboBox<>();
    private final JComboBox<String> typeCombo = new JComboBox<>(new String[]{
            "Routine Servicing",
            "Accident / Crash Repair",
            "Engine Diagnostics & Repair",
            "Brake Overhaul",
            "Oil & Filter Change",
            "Tire Replacement",
            "Electrical & Battery Service",
            "Periodic Inspection / MOT",
            "Body & Paint Work",
            "Other Maintenance"
    });
    private final JTextField serviceCenterField = new JTextField("City Auto Care Workshop");
    private final JTextField costField = new JTextField("0.00");
    private final JTextField startDateField = new JTextField(LocalDate.now().toString());
    private final JTextField endDateField = new JTextField();
    private final JComboBox<String> statusCombo = new JComboBox<>(new String[]{
            "UNDER_MAINTENANCE", "COMPLETED", "SCHEDULED"
    });
    private final JTextField conditionNotesField = new JTextField();
    private final JTextField descriptionField = new JTextField();

    // Top status indicator badge
    private final JLabel unavailableBannerLabel = new JLabel();

    // Filter controls
    private final JComboBox<String> viewFilterCombo = new JComboBox<>(new String[]{
            "ALL Records", "Currently Under Maintenance (Unavailable)", "Completed"
    });

    private final DefaultTableModel tableModel =
            new DefaultTableModel(new String[]{
                    "Maint ID", "Veh ID", "Registration", "Model",
                    "Type", "Servicing / Repair Details", "Cost (₹)",
                    "Start Date", "End Date", "Status", "Service Center", "Condition Notes"
            }, 0) {
                @Override
                public boolean isCellEditable(int row, int col) {
                    return false;
                }
            };

    private final JTable table = new JTable(tableModel);

    // Helper item to display vehicles nicely in dropdown
    public static class VehicleComboItem {
        public final Vehicle vehicle;

        public VehicleComboItem(Vehicle vehicle) {
            this.vehicle = vehicle;
        }

        @Override
        public String toString() {
            if (vehicle == null) return "-- Select Vehicle --";
            return String.format("#%d - %s (%s) [%s]",
                    vehicle.getVehicleId(),
                    vehicle.getModel(),
                    vehicle.getRegistrationNo(),
                    vehicle.getStatus());
        }
    }

    public MaintenancePanel() {
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // 1. Header Title & Unavailable Banner
        JPanel headerPanel = new JPanel(new BorderLayout(8, 8));

        JLabel title = new JLabel("Vehicle Maintenance & Servicing");
        title.setFont(new Font("SansSerif", Font.BOLD, 23));

        unavailableBannerLabel.setOpaque(true);
        unavailableBannerLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        unavailableBannerLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(217, 83, 79), 1),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        unavailableBannerLabel.setBackground(new Color(255, 235, 235));
        unavailableBannerLabel.setForeground(new Color(160, 20, 20));

        headerPanel.add(title, BorderLayout.WEST);
        headerPanel.add(unavailableBannerLabel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // 2. Form for recording maintenance
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.setBorder(BorderFactory.createTitledBorder("Record Servicing & Repair Details"));

        form.add(new JLabel("Select Vehicle:"));
        form.add(vehicleCombo);

        form.add(new JLabel("Maintenance Type:"));
        form.add(typeCombo);

        form.add(new JLabel("Service Center / Workshop:"));
        form.add(serviceCenterField);

        form.add(new JLabel("Estimated / Actual Cost (₹):"));
        form.add(costField);

        form.add(new JLabel("Start Date (YYYY-MM-DD):"));
        form.add(startDateField);

        form.add(new JLabel("Completion Date (optional, YYYY-MM-DD):"));
        form.add(endDateField);

        form.add(new JLabel("Maintenance Status:"));
        form.add(statusCombo);

        form.add(new JLabel("Vehicle Condition / Return Damage Notes:"));
        form.add(conditionNotesField);

        form.add(new JLabel("Work Done / Repair Description:"));
        form.add(descriptionField);

        JButton recordBtn = new JButton("Record Maintenance");
        recordBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        recordBtn.setBackground(new Color(39, 85, 145));
        recordBtn.setForeground(Color.WHITE);
        recordBtn.setFocusPainted(false);

        JButton completeBtn = new JButton("Complete Selected Maintenance");
        completeBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        completeBtn.setBackground(new Color(40, 167, 69));
        completeBtn.setForeground(Color.WHITE);
        completeBtn.setFocusPainted(false);

        form.add(recordBtn);
        form.add(completeBtn);

        // 3. Tools / Filter Bar
        JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        tools.add(new JLabel("Filter View:"));
        tools.add(viewFilterCombo);

        JButton refreshBtn = new JButton("Refresh");
        JButton searchBtn = new JButton("Search by Vehicle ID");
        JButton viewUnavailableBtn = new JButton("Show Unavailable Vehicles");
        JButton clearBtn = new JButton("Clear Form");

        tools.add(refreshBtn);
        tools.add(searchBtn);
        tools.add(viewUnavailableBtn);
        tools.add(clearBtn);

        JPanel topContainer = new JPanel(new BorderLayout(8, 8));
        topContainer.add(form, BorderLayout.CENTER);
        topContainer.add(tools, BorderLayout.SOUTH);

        // 4. Center Table
        table.setRowHeight(25);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Status column highlight renderer
        table.getColumnModel().getColumn(9).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
                if (value != null) {
                    String str = value.toString();
                    if ("UNDER_MAINTENANCE".equalsIgnoreCase(str)) {
                        c.setForeground(new Color(180, 40, 40));
                        setText("UNAVAILABLE (In Maintenance)");
                    } else if ("COMPLETED".equalsIgnoreCase(str)) {
                        c.setForeground(new Color(30, 130, 60));
                    } else if ("SCHEDULED".equalsIgnoreCase(str)) {
                        c.setForeground(new Color(0, 102, 204));
                    }
                }
                return c;
            }
        });

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.add(topContainer, BorderLayout.NORTH);
        content.add(new JScrollPane(table), BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);

        // Listeners
        recordBtn.addActionListener(e -> recordMaintenance());
        completeBtn.addActionListener(e -> completeSelectedMaintenance());
        refreshBtn.addActionListener(e -> {
            loadVehiclesDropdown();
            loadRecords();
            updateUnavailableBanner();
        });
        viewFilterCombo.addActionListener(e -> loadRecords());
        searchBtn.addActionListener(e -> searchByVehicleId());
        viewUnavailableBtn.addActionListener(e -> showUnavailableVehiclesDialog());
        clearBtn.addActionListener(e -> clearForm());

        // When a vehicle is chosen in dropdown, auto-populate condition notes from return history!
        vehicleCombo.addActionListener(e -> onVehicleSelected());

        // Table selection listener
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                int row = table.getSelectedRow();
                descriptionField.setText(cell(row, 5));
                costField.setText(cell(row, 6));
                startDateField.setText(cell(row, 7));
                endDateField.setText(cell(row, 8));
                serviceCenterField.setText(cell(row, 10));
                conditionNotesField.setText(cell(row, 11));

                String status = cell(row, 9);
                statusCombo.setSelectedItem(status);

                String vType = cell(row, 4);
                typeCombo.setSelectedItem(vType);

                int vId = Integer.parseInt(cell(row, 1));
                selectVehicleInDropdown(vId);
            }
        });

        loadVehiclesDropdown();
        loadRecords();
        updateUnavailableBanner();
    }

    private String cell(int row, int col) {
        Object val = tableModel.getValueAt(row, col);
        return val == null ? "" : val.toString();
    }

    public void loadVehiclesDropdown() {
        vehicleCombo.removeAllItems();
        vehicleCombo.addItem(new VehicleComboItem(null));
        try {
            List<Vehicle> vehicles = vehicleService.getAllVehicles();
            for (Vehicle v : vehicles) {
                vehicleCombo.addItem(new VehicleComboItem(v));
            }
        } catch (SQLException ex) {
            error(ex);
        }
    }

    private void selectVehicleInDropdown(int vehicleId) {
        for (int i = 0; i < vehicleCombo.getItemCount(); i++) {
            VehicleComboItem item = vehicleCombo.getItemAt(i);
            if (item != null && item.vehicle != null && item.vehicle.getVehicleId() == vehicleId) {
                vehicleCombo.setSelectedIndex(i);
                break;
            }
        }
    }

    private void onVehicleSelected() {
        VehicleComboItem item = (VehicleComboItem) vehicleCombo.getSelectedItem();
        if (item != null && item.vehicle != null) {
            try {
                // Fetch latest condition from return records
                String condition = maintenanceService.getVehicleConditionSummary(item.vehicle.getVehicleId());
                if (!condition.isEmpty()) {
                    conditionNotesField.setText(condition);
                } else {
                    conditionNotesField.setText("Vehicle in " + item.vehicle.getStatus().toLowerCase() + " condition");
                }
            } catch (SQLException ignored) {}
        }
    }

    public void updateUnavailableBanner() {
        try {
            List<Vehicle> unavailable = maintenanceService.getUnavailableVehiclesDueToMaintenance();
            int count = unavailable.size();
            if (count == 0) {
                unavailableBannerLabel.setText("✔ All Fleet Active (0 Vehicles Under Maintenance)");
                unavailableBannerLabel.setBackground(new Color(235, 250, 235));
                unavailableBannerLabel.setForeground(new Color(30, 130, 60));
                unavailableBannerLabel.setBorder(BorderFactory.createLineBorder(new Color(40, 167, 69), 1));
            } else {
                StringBuilder names = new StringBuilder();
                for (int i = 0; i < Math.min(count, 3); i++) {
                    if (i > 0) names.append(", ");
                    names.append(unavailable.get(i).getModel())
                         .append(" [").append(unavailable.get(i).getRegistrationNo()).append("]");
                }
                if (count > 3) names.append("...");

                unavailableBannerLabel.setText(String.format(
                        "⚠️ %d Vehicle%s Unavailable due to Maintenance (%s)",
                        count, count > 1 ? "s" : "", names.toString()
                ));
                unavailableBannerLabel.setBackground(new Color(255, 235, 235));
                unavailableBannerLabel.setForeground(new Color(160, 20, 20));
                unavailableBannerLabel.setBorder(BorderFactory.createLineBorder(new Color(217, 83, 79), 1));
            }
        } catch (SQLException ex) {
            unavailableBannerLabel.setText("Status unavailable");
        }
    }

    public void loadRecords() {
        try {
            String filter = (String) viewFilterCombo.getSelectedItem();
            List<MaintenanceRecord> records;

            if ("Currently Under Maintenance (Unavailable)".equals(filter)) {
                records = maintenanceService.getActiveMaintenance();
            } else if ("Completed".equals(filter)) {
                records = maintenanceService.getAllMaintenance().stream()
                        .filter(r -> "COMPLETED".equalsIgnoreCase(r.getStatus()))
                        .toList();
            } else {
                records = maintenanceService.getAllMaintenance();
            }

            display(records);
            updateUnavailableBanner();
        } catch (SQLException ex) {
            error(ex);
        }
    }

    private void display(List<MaintenanceRecord> records) {
        tableModel.setRowCount(0);
        for (MaintenanceRecord r : records) {
            tableModel.addRow(new Object[]{
                    r.getMaintenanceId(),
                    r.getVehicleId(),
                    r.getVehicleRegistration(),
                    r.getVehicleModel(),
                    r.getMaintenanceType(),
                    r.getDescription(),
                    String.format("%.2f", r.getCost()),
                    r.getStartDate(),
                    r.getEndDate() == null ? "-" : r.getEndDate().toString(),
                    r.getStatus(),
                    r.getServiceCenter(),
                    r.getConditionNotes()
            });
        }
    }

    private void recordMaintenance() {
        VehicleComboItem selected = (VehicleComboItem) vehicleCombo.getSelectedItem();
        if (selected == null || selected.vehicle == null) {
            message("Please select a vehicle to log maintenance.");
            return;
        }

        String type = (String) typeCombo.getSelectedItem();
        String workshop = serviceCenterField.getText().trim();
        String costStr = costField.getText().trim();
        String startStr = startDateField.getText().trim();
        String endStr = endDateField.getText().trim();
        String status = (String) statusCombo.getSelectedItem();
        String condition = conditionNotesField.getText().trim();
        String desc = descriptionField.getText().trim();

        if (startStr.isEmpty()) {
            message("Start date (YYYY-MM-DD) is required.");
            return;
        }

        if (desc.isEmpty()) {
            message("Please enter servicing / repair details and work description.");
            return;
        }

        try {
            double cost = costStr.isEmpty() ? 0.0 : Double.parseDouble(costStr);
            Date startDate = Date.valueOf(startStr);
            Date endDate = endStr.isEmpty() ? null : Date.valueOf(endStr);

            if (endDate != null && endDate.before(startDate)) {
                message("Completion date cannot be earlier than start date.");
                return;
            }

            MaintenanceRecord record = new MaintenanceRecord(
                    selected.vehicle.getVehicleId(),
                    type,
                    desc,
                    cost,
                    startDate,
                    endDate,
                    status != null ? status : "UNDER_MAINTENANCE",
                    workshop,
                    condition
            );

            if (maintenanceService.recordMaintenance(record)) {
                String notice = "Maintenance recorded successfully!\n\n" +
                        "Maintenance ID: #" + record.getMaintenanceId() + "\n" +
                        "Vehicle: " + selected.vehicle.getModel() + " (" + selected.vehicle.getRegistrationNo() + ")\n" +
                        "Type: " + type + "\n" +
                        "Status: " + record.getStatus();

                if ("UNDER_MAINTENANCE".equalsIgnoreCase(record.getStatus())) {
                    notice += "\n\nVehicle status has been updated to 'MAINTENANCE'.\n" +
                              "The vehicle is now indicated as UNAVAILABLE for rental.";
                }

                JOptionPane.showMessageDialog(this, notice, "Maintenance Logged", JOptionPane.INFORMATION_MESSAGE);

                clearForm();
                loadVehiclesDropdown();
                loadRecords();
                updateUnavailableBanner();
            } else {
                message("Could not save maintenance record.");
            }

        } catch (NumberFormatException ex) {
            message("Cost must be a valid numeric amount.");
        } catch (IllegalArgumentException ex) {
            message("Validation error: " + ex.getMessage());
        } catch (SQLException ex) {
            error(ex);
        }
    }

    private void completeSelectedMaintenance() {
        int row = table.getSelectedRow();
        if (row < 0) {
            message("Please select a maintenance record from the table to complete.");
            return;
        }

        int maintId = Integer.parseInt(cell(row, 0));
        String currentStatus = cell(row, 9);
        if ("COMPLETED".equalsIgnoreCase(currentStatus)) {
            message("Maintenance record #" + maintId + " is already COMPLETED.");
            return;
        }

        String reg = cell(row, 2);
        String model = cell(row, 3);
        String existingCost = cell(row, 6);

        JTextField costInput = new JTextField(existingCost);
        JTextField dateInput = new JTextField(LocalDate.now().toString());
        JTextField notesInput = new JTextField("Servicing complete, vehicle tested and roadworthy");

        JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
        form.add(new JLabel("Complete Maintenance for " + model + " (" + reg + ")"));
        form.add(new JLabel("Final Cost (₹):"));
        form.add(costInput);
        form.add(new JLabel("Completion Date (YYYY-MM-DD):"));
        form.add(dateInput);
        form.add(new JLabel("Completion Notes / Work Certified:"));
        form.add(notesInput);

        int result = JOptionPane.showConfirmDialog(
                this, form, "Complete Maintenance & Restore Vehicle",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            double finalCost = Double.parseDouble(costInput.getText().trim());
            Date completionDate = Date.valueOf(dateInput.getText().trim());
            String notes = notesInput.getText().trim();

            if (maintenanceService.completeMaintenance(maintId, completionDate, finalCost, notes)) {
                JOptionPane.showMessageDialog(
                        this,
                        "Maintenance #" + maintId + " Completed Successfully!\n\n" +
                        "Vehicle: " + model + " (" + reg + ")\n" +
                        "Completion Date: " + completionDate + "\n" +
                        "Final Cost: ₹" + String.format("%.2f", finalCost) + "\n\n" +
                        "Vehicle status has been restored to 'AVAILABLE' for customer booking.",
                        "Maintenance Complete",
                        JOptionPane.INFORMATION_MESSAGE
                );

                loadVehiclesDropdown();
                loadRecords();
                updateUnavailableBanner();
            } else {
                message("Could not complete maintenance.");
            }

        } catch (NumberFormatException ex) {
            message("Final cost must be a valid number.");
        } catch (IllegalArgumentException ex) {
            message("Validation error: " + ex.getMessage());
        } catch (SQLException ex) {
            error(ex);
        }
    }

    private void searchByVehicleId() {
        String input = JOptionPane.showInputDialog(this, "Enter Vehicle ID to view maintenance history:");
        if (input == null || input.trim().isEmpty()) return;

        try {
            int vId = Integer.parseInt(input.trim());
            List<MaintenanceRecord> records = maintenanceService.getMaintenanceByVehicleId(vId);
            tableModel.setRowCount(0);
            if (records.isEmpty()) {
                message("No maintenance records found for Vehicle #" + vId + ".");
            } else {
                display(records);
            }
        } catch (NumberFormatException ex) {
            message("Vehicle ID must be a whole number.");
        } catch (SQLException ex) {
            error(ex);
        }
    }

    private void showUnavailableVehiclesDialog() {
        try {
            List<Vehicle> unavailable = maintenanceService.getUnavailableVehiclesDueToMaintenance();
            if (unavailable.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "All fleet vehicles are currently active!\nNo vehicles are unavailable due to maintenance.",
                        "Unavailable Vehicles",
                        JOptionPane.INFORMATION_MESSAGE
                );
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Vehicles Currently Unavailable Due to Maintenance (").append(unavailable.size()).append("):\n\n");
            for (Vehicle v : unavailable) {
                sb.append("• Vehicle #").append(v.getVehicleId())
                  .append(" - ").append(v.getModel())
                  .append(" (").append(v.getRegistrationNo()).append(")")
                  .append(" | Category: ").append(v.getCategory())
                  .append(" | Status: ").append(v.getStatus())
                  .append("\n");

                // Include latest maintenance notes if any
                List<MaintenanceRecord> recs = maintenanceService.getMaintenanceByVehicleId(v.getVehicleId());
                if (!recs.isEmpty()) {
                    MaintenanceRecord latest = recs.get(0);
                    sb.append("   └ Work: ").append(latest.getMaintenanceType())
                      .append(" - ").append(latest.getDescription())
                      .append(" [Started: ").append(latest.getStartDate()).append("]\n");
                }
                sb.append("\n");
            }

            JTextArea area = new JTextArea(sb.toString(), 14, 45);
            area.setEditable(false);
            area.setFont(new Font("Monospaced", Font.PLAIN, 12));
            JOptionPane.showMessageDialog(this, new JScrollPane(area),
                    "Unavailable Vehicles Under Maintenance", JOptionPane.WARNING_MESSAGE);

        } catch (SQLException ex) {
            error(ex);
        }
    }

    private void clearForm() {
        if (vehicleCombo.getItemCount() > 0) vehicleCombo.setSelectedIndex(0);
        typeCombo.setSelectedIndex(0);
        serviceCenterField.setText("City Auto Care Workshop");
        costField.setText("0.00");
        startDateField.setText(LocalDate.now().toString());
        endDateField.setText("");
        statusCombo.setSelectedIndex(0);
        conditionNotesField.setText("");
        descriptionField.setText("");
        table.clearSelection();
    }

    private void message(String text) {
        JOptionPane.showMessageDialog(this, text);
    }

    private void error(Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
