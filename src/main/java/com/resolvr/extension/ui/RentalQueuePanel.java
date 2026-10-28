package com.resolvr.extension.ui;

import com.resolvr.extension.model.RentalQueueEntry;
import com.resolvr.extension.service.RentalQueueService;
import com.resolvr.model.Customer;
import com.resolvr.model.Rental;
import com.resolvr.model.Vehicle;
import com.resolvr.service.CustomerService;
import com.resolvr.service.VehicleService;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class RentalQueuePanel extends JPanel {

    private final RentalQueueService queueService = new RentalQueueService();
    private final VehicleService vehicleService = new VehicleService();
    private final CustomerService customerService = new CustomerService();

    // Form inputs
    private final JComboBox<VehicleComboItem> vehicleCombo = new JComboBox<>();
    private final JComboBox<CustomerComboItem> customerCombo = new JComboBox<>();
    private final JTextField startDateField = new JTextField(LocalDate.now().toString());
    private final JTextField endDateField = new JTextField(LocalDate.now().plusDays(1).toString());
    private final JSpinner hoursSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 720, 1));
    private final JTextField estimatedAmountField = new JTextField("0.00");
    private final JTextField notesField = new JTextField();

    // Top status indicator banner
    private final JLabel queueBannerLabel = new JLabel();

    // Filter controls
    private final JComboBox<String> viewFilterCombo = new JComboBox<>(new String[]{
            "ALL Records", "Waiting Only (Active Queue)", "Promoted to Rental", "Cancelled"
    });

    private final DefaultTableModel tableModel =
            new DefaultTableModel(new String[]{
                    "Queue ID", "Pos #", "Veh ID", "Vehicle Info",
                    "Cust ID", "Customer Details", "Start Date", "End Date",
                    "Hours", "Est Amount (₹)", "Status", "Requested On", "Notes"
            }, 0) {
                @Override
                public boolean isCellEditable(int row, int col) {
                    return false;
                }
            };

    private final JTable table = new JTable(tableModel);

    public static class VehicleComboItem {
        public final Vehicle vehicle;

        public VehicleComboItem(Vehicle vehicle) {
            this.vehicle = vehicle;
        }

        @Override
        public String toString() {
            if (vehicle == null) return "-- Select Vehicle --";
            return String.format("#%d - %s (%s) [%s - ₹%.2f/hr]",
                    vehicle.getVehicleId(),
                    vehicle.getModel(),
                    vehicle.getRegistrationNo(),
                    vehicle.getStatus(),
                    vehicle.getPricePerHour());
        }
    }

    public static class CustomerComboItem {
        public final Customer customer;

        public CustomerComboItem(Customer customer) {
            this.customer = customer;
        }

        @Override
        public String toString() {
            if (customer == null) return "-- Select Customer --";
            return String.format("#%d - %s (%s)",
                    customer.getCustomerId(),
                    customer.getName(),
                    customer.getLicenseNo());
        }
    }

    public RentalQueuePanel() {
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // 1. Header Title & Queue Banner
        JPanel headerPanel = new JPanel(new BorderLayout(8, 8));

        JLabel title = new JLabel("Vehicle Rental Priority Queue & Waitlist");
        title.setFont(new Font("SansSerif", Font.BOLD, 23));

        queueBannerLabel.setOpaque(true);
        queueBannerLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        queueBannerLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 126, 34), 1),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        queueBannerLabel.setBackground(new Color(254, 249, 231));
        queueBannerLabel.setForeground(new Color(175, 96, 26));

        headerPanel.add(title, BorderLayout.WEST);
        headerPanel.add(queueBannerLabel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // 2. Form for adding customer to waitlist / queue
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.setBorder(BorderFactory.createTitledBorder("Add Customer to Vehicle Waiting List / Queue"));

        form.add(new JLabel("Select Vehicle:"));
        form.add(vehicleCombo);

        form.add(new JLabel("Select Customer:"));
        form.add(customerCombo);

        form.add(new JLabel("Start Date (YYYY-MM-DD):"));
        form.add(startDateField);

        form.add(new JLabel("End Date (YYYY-MM-DD):"));
        form.add(endDateField);

        form.add(new JLabel("Duration (Hours):"));
        form.add(hoursSpinner);

        form.add(new JLabel("Estimated Cost (₹):"));
        form.add(estimatedAmountField);

        form.add(new JLabel("Queue Priority Notes / Remarks:"));
        form.add(notesField);

        JButton addToQueueBtn = new JButton("Add to Waitlist Queue");
        addToQueueBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        addToQueueBtn.setBackground(new Color(39, 85, 145));
        addToQueueBtn.setForeground(Color.WHITE);
        addToQueueBtn.setFocusPainted(false);

        JButton promoteBtn = new JButton("Promote Next in Queue (Take Vehicle)");
        promoteBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        promoteBtn.setBackground(new Color(40, 167, 69));
        promoteBtn.setForeground(Color.WHITE);
        promoteBtn.setFocusPainted(false);

        form.add(addToQueueBtn);
        form.add(promoteBtn);

        // 3. Tools / Filter Bar
        JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        tools.add(new JLabel("Filter View:"));
        tools.add(viewFilterCombo);

        JButton refreshBtn = new JButton("Refresh Queue");
        JButton cancelQueueBtn = new JButton("Cancel Selected Waitlist Entry");
        JButton viewDetailsBtn = new JButton("View Entry Details");
        JButton clearBtn = new JButton("Clear Form");

        tools.add(refreshBtn);
        tools.add(cancelQueueBtn);
        tools.add(viewDetailsBtn);
        tools.add(clearBtn);

        JPanel topContainer = new JPanel(new BorderLayout(8, 8));
        topContainer.add(form, BorderLayout.CENTER);
        topContainer.add(tools, BorderLayout.SOUTH);

        // 4. Center Table
        table.setRowHeight(25);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Status column highlight renderer (column 10)
        table.getColumnModel().getColumn(10).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
                if (value != null) {
                    String str = value.toString();
                    if ("WAITING".equalsIgnoreCase(str)) {
                        c.setForeground(new Color(211, 84, 0));
                        Object posVal = tbl.getValueAt(row, 1);
                        setText("WAITING (Position #" + posVal + ")");
                    } else if ("PROMOTED".equalsIgnoreCase(str)) {
                        c.setForeground(new Color(39, 174, 96));
                        setText("PROMOTED (Booked)");
                    } else if ("CANCELLED".equalsIgnoreCase(str)) {
                        c.setForeground(new Color(127, 140, 141));
                        setText("CANCELLED");
                    }
                }
                return c;
            }
        });

        // Position column alignment
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.add(topContainer, BorderLayout.NORTH);
        content.add(new JScrollPane(table), BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);

        // Listeners
        addToQueueBtn.addActionListener(e -> addToQueue());
        promoteBtn.addActionListener(e -> promoteNextInQueue());
        cancelQueueBtn.addActionListener(e -> cancelSelectedEntry());
        viewDetailsBtn.addActionListener(e -> viewDetails());
        refreshBtn.addActionListener(e -> {
            loadDropdowns();
            loadRecords();
            updateQueueBanner();
        });
        viewFilterCombo.addActionListener(e -> loadRecords());
        clearBtn.addActionListener(e -> clearForm());

        vehicleCombo.addActionListener(e -> calculateEstimate());
        hoursSpinner.addChangeListener(e -> calculateEstimate());

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                int row = table.getSelectedRow();
                try {
                    int vId = Integer.parseInt(cell(row, 2));
                    int cId = Integer.parseInt(cell(row, 4));
                    selectVehicleInDropdown(vId);
                    selectCustomerInDropdown(cId);
                    startDateField.setText(cell(row, 6));
                    endDateField.setText(cell(row, 7));
                    hoursSpinner.setValue(Integer.parseInt(cell(row, 8)));
                    estimatedAmountField.setText(cell(row, 9));
                    notesField.setText(cell(row, 12));
                } catch (Exception ignored) {}
            }
        });

        loadDropdowns();
        loadRecords();
        updateQueueBanner();
    }

    private String cell(int row, int col) {
        Object val = tableModel.getValueAt(row, col);
        return val == null ? "" : val.toString();
    }

    public void loadDropdowns() {
        try {
            // Load vehicles
            VehicleComboItem currentV = (VehicleComboItem) vehicleCombo.getSelectedItem();
            int selVId = (currentV != null && currentV.vehicle != null) ? currentV.vehicle.getVehicleId() : -1;

            vehicleCombo.removeAllItems();
            vehicleCombo.addItem(new VehicleComboItem(null));
            List<Vehicle> vehicles = vehicleService.getAllVehicles();
            int vMatchIdx = 0;
            for (int i = 0; i < vehicles.size(); i++) {
                Vehicle v = vehicles.get(i);
                vehicleCombo.addItem(new VehicleComboItem(v));
                if (v.getVehicleId() == selVId) {
                    vMatchIdx = i + 1;
                }
            }
            vehicleCombo.setSelectedIndex(vMatchIdx);

            // Load customers
            CustomerComboItem currentC = (CustomerComboItem) customerCombo.getSelectedItem();
            int selCId = (currentC != null && currentC.customer != null) ? currentC.customer.getCustomerId() : -1;

            customerCombo.removeAllItems();
            customerCombo.addItem(new CustomerComboItem(null));
            List<Customer> customers = customerService.getAllCustomers();
            int cMatchIdx = 0;
            for (int i = 0; i < customers.size(); i++) {
                Customer c = customers.get(i);
                customerCombo.addItem(new CustomerComboItem(c));
                if (c.getCustomerId() == selCId) {
                    cMatchIdx = i + 1;
                }
            }
            customerCombo.setSelectedIndex(cMatchIdx);

        } catch (SQLException ex) {
            error(ex);
        }
    }

    public void updateQueueBanner() {
        try {
            int activeEntries = queueService.getActiveQueueCount();
            int activeVehicles = queueService.getActiveVehiclesInQueueCount();

            if (activeEntries == 0) {
                queueBannerLabel.setText("WAITLIST QUEUE: Empty (All requests fulfilled)");
                queueBannerLabel.setBackground(new Color(240, 248, 255));
                queueBannerLabel.setForeground(new Color(70, 130, 180));
                queueBannerLabel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(176, 196, 222), 1),
                        BorderFactory.createEmptyBorder(6, 12, 6, 12)
                ));
            } else {
                queueBannerLabel.setText(String.format("WAITLIST ACTIVE: %d Customer(s) in Queue across %d Vehicle(s)",
                        activeEntries, activeVehicles));
                queueBannerLabel.setBackground(new Color(254, 249, 231));
                queueBannerLabel.setForeground(new Color(175, 96, 26));
                queueBannerLabel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(230, 126, 34), 1),
                        BorderFactory.createEmptyBorder(6, 12, 6, 12)
                ));
            }
        } catch (SQLException ex) {
            queueBannerLabel.setText("Waitlist status unavailable");
        }
    }

    public void loadRecords() {
        try {
            List<RentalQueueEntry> list = queueService.getAllQueueEntries();
            String filter = (String) viewFilterCombo.getSelectedItem();

            tableModel.setRowCount(0);
            for (RentalQueueEntry q : list) {
                if ("Waiting Only (Active Queue)".equals(filter) && !"WAITING".equalsIgnoreCase(q.getStatus())) {
                    continue;
                }
                if ("Promoted to Rental".equals(filter) && !"PROMOTED".equalsIgnoreCase(q.getStatus())) {
                    continue;
                }
                if ("Cancelled".equals(filter) && !"CANCELLED".equalsIgnoreCase(q.getStatus())) {
                    continue;
                }

                String vInfo = q.getVehicleModel() + " (" + q.getVehicleRegistration() + ")";
                String cInfo = q.getCustomerName() + " (" + (q.getCustomerPhone() != null ? q.getCustomerPhone() : "No Phone") + ")";

                tableModel.addRow(new Object[]{
                        q.getQueueId(),
                        q.getQueuePosition(),
                        q.getVehicleId(),
                        vInfo,
                        q.getCustomerId(),
                        cInfo,
                        q.getStartDate(),
                        q.getEndDate(),
                        q.getDurationHours(),
                        String.format("%.2f", q.getEstimatedAmount()),
                        q.getStatus(),
                        q.getCreatedAt() != null ? q.getCreatedAt().toString() : "N/A",
                        q.getNotes() != null ? q.getNotes() : ""
                });
            }
        } catch (SQLException ex) {
            error(ex);
        }
    }

    private void calculateEstimate() {
        VehicleComboItem item = (VehicleComboItem) vehicleCombo.getSelectedItem();
        if (item == null || item.vehicle == null) {
            estimatedAmountField.setText("0.00");
            return;
        }

        try {
            int hours = ((Number) hoursSpinner.getValue()).intValue();
            if (hours <= 0) hours = 1;
            double amount = hours * item.vehicle.getPricePerHour();
            estimatedAmountField.setText(String.format("%.2f", amount));
        } catch (Exception ex) {
            estimatedAmountField.setText("0.00");
        }
    }

    private void addToQueue() {
        VehicleComboItem vItem = (VehicleComboItem) vehicleCombo.getSelectedItem();
        if (vItem == null || vItem.vehicle == null) {
            message("Please select a vehicle.");
            return;
        }

        CustomerComboItem cItem = (CustomerComboItem) customerCombo.getSelectedItem();
        if (cItem == null || cItem.customer == null) {
            message("Please select a customer.");
            return;
        }

        try {
            Date start = Date.valueOf(startDateField.getText().trim());
            Date end = Date.valueOf(endDateField.getText().trim());
            int hours = ((Number) hoursSpinner.getValue()).intValue();
            double amount = Double.parseDouble(estimatedAmountField.getText().trim());
            String notes = notesField.getText().trim();

            RentalQueueEntry entry = new RentalQueueEntry(
                    vItem.vehicle.getVehicleId(),
                    cItem.customer.getCustomerId(),
                    start,
                    end,
                    hours,
                    amount,
                    notes.isEmpty() ? "Added via Management Portal" : notes
            );

            if (queueService.joinQueue(entry)) {
                JOptionPane.showMessageDialog(
                        this,
                        "Customer added to Waitlist Queue successfully!\n\n" +
                        "Customer: " + cItem.customer.getName() + "\n" +
                        "Vehicle: " + vItem.vehicle.getModel() + " (" + vItem.vehicle.getRegistrationNo() + ")\n" +
                        "Queue Position: #" + entry.getQueuePosition() + " (Priority Order)\n" +
                        "Status: WAITING\n\n" +
                        "When the current rental is cancelled, priority automatically transfers to this customer!",
                        "Queue Entry Added",
                        JOptionPane.INFORMATION_MESSAGE
                );
                loadRecords();
                updateQueueBanner();
                clearForm();
            } else {
                message("Could not add customer to queue.");
            }

        } catch (IllegalArgumentException ex) {
            message("Validation Error: " + ex.getMessage());
        } catch (SQLException ex) {
            error(ex);
        }
    }

    private void promoteNextInQueue() {
        VehicleComboItem vItem = (VehicleComboItem) vehicleCombo.getSelectedItem();
        int vId = -1;
        if (vItem != null && vItem.vehicle != null) {
            vId = vItem.vehicle.getVehicleId();
        } else {
            // Check table selection
            int row = table.getSelectedRow();
            if (row >= 0) {
                vId = Integer.parseInt(cell(row, 2));
            }
        }

        if (vId <= 0) {
            message("Select a vehicle from the dropdown or select a queue entry from the table first.");
            return;
        }

        try {
            RentalQueueEntry next = queueService.getQueueForVehicle(vId).stream().findFirst().orElse(null);
            if (next == null) {
                message("No customers currently waiting in queue for vehicle #" + vId + ".");
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Promote Next Customer in Queue:\n\n" +
                    "Vehicle: " + next.getVehicleModel() + " (" + next.getVehicleRegistration() + ")\n" +
                    "Next in Line (Pos #1): " + next.getCustomerName() + " (ID #" + next.getCustomerId() + ")\n" +
                    "Requested Dates: " + next.getStartDate() + " to " + next.getEndDate() + "\n" +
                    "Amount: ₹" + String.format("%.2f", next.getEstimatedAmount()) + "\n\n" +
                    "Allocate vehicle to this customer and create booking now?",
                    "Confirm Queue Promotion",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );

            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }

            Rental newRental = queueService.promoteNextInQueue(vId);
            if (newRental != null) {
                JOptionPane.showMessageDialog(
                        this,
                        "Queue Promotion Successful!\n\n" +
                        "Rental #" + newRental.getRentalId() + " has been created for " + next.getCustomerName() + ".\n" +
                        "Vehicle status updated to RENTED.\n" +
                        "Customer #" + next.getCustomerId() + " has taken the vehicle!",
                        "Vehicle Allocated",
                        JOptionPane.INFORMATION_MESSAGE
                );
                loadRecords();
                loadDropdowns();
                updateQueueBanner();
            } else {
                message("Promotion could not be completed.");
            }

        } catch (Exception ex) {
            error(ex);
        }
    }

    private void cancelSelectedEntry() {
        int row = table.getSelectedRow();
        if (row < 0) {
            message("Select a queue entry from the table first.");
            return;
        }

        int qId = Integer.parseInt(cell(row, 0));
        String currentStatus = cell(row, 10);
        String custInfo = cell(row, 5);
        String vehInfo = cell(row, 3);

        if ("CANCELLED".equalsIgnoreCase(currentStatus)) {
            message("This waitlist entry is already cancelled.");
            return;
        }

        if ("PROMOTED".equalsIgnoreCase(currentStatus)) {
            message("Cannot cancel an entry that was already promoted to an active rental.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to cancel the waitlist entry for:\n" +
                custInfo + " for vehicle " + vehInfo + "?",
                "Confirm Cancellation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            if (queueService.cancelQueueEntry(qId)) {
                message("Waitlist Entry #" + qId + " has been cancelled. Remaining queue positions updated.");
                loadRecords();
                updateQueueBanner();
            } else {
                message("Could not cancel entry.");
            }
        } catch (Exception ex) {
            error(ex);
        }
    }

    private void viewDetails() {
        int row = table.getSelectedRow();
        if (row < 0) {
            message("Select a queue entry from the table first.");
            return;
        }

        int qId = Integer.parseInt(cell(row, 0));
        try {
            RentalQueueEntry q = queueService.getQueueEntryById(qId);
            if (q == null) {
                message("Entry not found.");
                return;
            }

            JOptionPane.showMessageDialog(
                    this,
                    "Waitlist Queue Entry Details:\n\n" +
                    "Queue ID: #" + q.getQueueId() + "\n" +
                    "Queue Position: Position #" + q.getQueuePosition() + " (Priority Order)\n" +
                    "Status: " + q.getStatus() + "\n\n" +
                    "Vehicle: #" + q.getVehicleId() + " - " + q.getVehicleModel() + " (" + q.getVehicleRegistration() + ")\n" +
                    "Customer: #" + q.getCustomerId() + " - " + q.getCustomerName() + " (" + q.getCustomerPhone() + ")\n\n" +
                    "Dates: " + q.getStartDate() + " to " + q.getEndDate() + " (" + q.getDurationHours() + " hrs)\n" +
                    "Estimated Total: ₹" + String.format("%.2f", q.getEstimatedAmount()) + "\n" +
                    "Requested At: " + q.getCreatedAt() + "\n" +
                    "Notes: " + (q.getNotes() != null ? q.getNotes() : "None") + "\n",
                    "Queue Entry #" + qId,
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (SQLException ex) {
            error(ex);
        }
    }

    private void selectVehicleInDropdown(int vehicleId) {
        for (int i = 0; i < vehicleCombo.getItemCount(); i++) {
            VehicleComboItem item = vehicleCombo.getItemAt(i);
            if (item != null && item.vehicle != null && item.vehicle.getVehicleId() == vehicleId) {
                vehicleCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void selectCustomerInDropdown(int customerId) {
        for (int i = 0; i < customerCombo.getItemCount(); i++) {
            CustomerComboItem item = customerCombo.getItemAt(i);
            if (item != null && item.customer != null && item.customer.getCustomerId() == customerId) {
                customerCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void clearForm() {
        vehicleCombo.setSelectedIndex(0);
        customerCombo.setSelectedIndex(0);
        startDateField.setText(LocalDate.now().toString());
        endDateField.setText(LocalDate.now().plusDays(1).toString());
        hoursSpinner.setValue(4);
        estimatedAmountField.setText("0.00");
        notesField.setText("");
        table.clearSelection();
    }

    private void message(String text) {
        JOptionPane.showMessageDialog(this, text);
    }

    private void error(Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
