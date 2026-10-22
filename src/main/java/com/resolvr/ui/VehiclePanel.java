package com.resolvr.ui;

import com.resolvr.model.Vehicle;
import com.resolvr.service.VehicleService;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class VehiclePanel extends JPanel {
    private final VehicleService service = new VehicleService();

    private final JTextField reg = new JTextField();
    private final JTextField model = new JTextField();
    private final JTextField price = new JTextField();
    private final JComboBox<String> category = new JComboBox<>(
            new String[]{"Car", "SUV", "Van", "Bike", "Other"});
    private final JComboBox<String> filter = new JComboBox<>(
            new String[]{"ALL", "Car", "SUV", "Van", "Bike", "Other"});

    private final DefaultTableModel tableModel =
            new DefaultTableModel(new String[]{
                    "ID", "Registration", "Model",
                    "Category", "Price/Hour (₹)", "Status"
            }, 0) {
                @Override
                public boolean isCellEditable(int row, int col) {
                    return false;
                }
            };

    private final JTable table = new JTable(tableModel);

    public VehiclePanel() {
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Vehicle Management");
        title.setFont(new Font("SansSerif", Font.BOLD, 23));
        add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.setBorder(BorderFactory.createTitledBorder("Vehicle Details"));
        form.add(new JLabel("Registration number"));
        form.add(reg);
        form.add(new JLabel("Model"));
        form.add(model);
        form.add(new JLabel("Category"));
        form.add(category);
        form.add(new JLabel("Hourly price (₹)"));
        form.add(price);

        JButton add = new JButton("Add Vehicle");
        JButton refresh = new JButton("Refresh");
        form.add(add);
        form.add(refresh);

        JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton search = new JButton("Filter");
        JButton updatePrice = new JButton("Update Price");
        JButton updateStatus = new JButton("Update Status");
        tools.add(new JLabel("Category:"));
        tools.add(filter);
        tools.add(search);
        tools.add(updatePrice);
        tools.add(updateStatus);

        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.add(form, BorderLayout.CENTER);
        top.add(tools, BorderLayout.SOUTH);

        table.setRowHeight(25);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Highlight maintenance and unavailable status
        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
                if (value != null) {
                    String str = value.toString();
                    if ("MAINTENANCE".equalsIgnoreCase(str)) {
                        c.setForeground(new Color(180, 40, 40));
                        setText("MAINTENANCE (Unavailable)");
                    } else if ("AVAILABLE".equalsIgnoreCase(str)) {
                        c.setForeground(new Color(30, 130, 60));
                    } else if ("RENTED".equalsIgnoreCase(str)) {
                        c.setForeground(new Color(0, 102, 204));
                    }
                }
                return c;
            }
        });

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.add(top, BorderLayout.NORTH);
        content.add(new JScrollPane(table), BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);

        add.addActionListener(e -> addVehicle());
        refresh.addActionListener(e -> loadVehicles());
        search.addActionListener(e -> searchVehicles());
        updatePrice.addActionListener(e -> updatePrice());
        updateStatus.addActionListener(e -> updateStatus());

        loadVehicles();
    }

    private void addVehicle() {
        try {
            String registration = reg.getText().trim();
            String vehicleModel = model.getText().trim();
            String vehicleCategory = (String) category.getSelectedItem();

            if (registration.isEmpty() || vehicleModel.isEmpty()
                    || price.getText().trim().isEmpty()) {
                message("Please fill in all vehicle fields.");
                return;
            }

            double hourlyPrice = Double.parseDouble(price.getText().trim());

            Vehicle vehicle = new Vehicle(
                    registration, vehicleModel, vehicleCategory, hourlyPrice);

            if (service.addVehicle(vehicle)) {
                message("Vehicle added successfully.");
                reg.setText("");
                model.setText("");
                price.setText("");
                loadVehicles();
            } else {
                message("Vehicle could not be added.");
            }
        } catch (NumberFormatException ex) {
            message("Enter a valid hourly price.");
        } catch (SQLException | IllegalArgumentException ex) {
            error(ex);
        }
    }

    public void loadVehicles() {
        try {
            display(service.getAllVehicles());
        } catch (SQLException ex) {
            error(ex);
        }
    }

    private void searchVehicles() {
        String selected = (String) filter.getSelectedItem();

        if ("ALL".equals(selected)) {
            loadVehicles();
            return;
        }

        try {
            display(service.searchByCategory(selected));
        } catch (SQLException | IllegalArgumentException ex) {
            error(ex);
        }
    }

    private void display(List<Vehicle> vehicles) {
        tableModel.setRowCount(0);
        for (Vehicle v : vehicles) {
            tableModel.addRow(new Object[]{
                    v.getVehicleId(), v.getRegistrationNo(),
                    v.getModel(), v.getCategory(),
                    String.format("%.2f", v.getPricePerHour()), v.getStatus()
            });
        }
    }

    private int selectedId() {
        int row = table.getSelectedRow();
        if (row < 0) {
            message("Select a vehicle from the table first.");
            return -1;
        }
        return Integer.parseInt(tableModel.getValueAt(row, 0).toString());
    }

    private void updatePrice() {
        int id = selectedId();
        if (id < 0) return;

        String value = JOptionPane.showInputDialog(
                this, "Enter the new hourly price (₹):");
        if (value == null) return;

        try {
            if (service.updatePrice(id, Double.parseDouble(value.trim()))) {
                message("Price updated.");
                loadVehicles();
            } else {
                message("Price was not updated.");
            }
        } catch (NumberFormatException ex) {
            message("Enter a valid price.");
        } catch (SQLException | IllegalArgumentException ex) {
            error(ex);
        }
    }

    private void updateStatus() {
        int id = selectedId();
        if (id < 0) return;

        String[] statuses = {"AVAILABLE", "RENTED", "MAINTENANCE"};
        String status = (String) JOptionPane.showInputDialog(
                this, "Choose vehicle status:", "Update Status",
                JOptionPane.QUESTION_MESSAGE, null, statuses, statuses[0]);

        if (status == null) return;

        try {
            if (service.updateStatus(id, status)) {
                message("Status updated.");
                loadVehicles();
            } else {
                message("Status was not updated.");
            }
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
