package com.resolvr.ui;

import com.resolvr.extension.model.RentalQueueEntry;
import com.resolvr.extension.service.RentalQueueService;
import com.resolvr.model.Customer;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class CustomerQueuePanel extends JPanel {

    private final Customer customer;
    private final RentalQueueService queueService = new RentalQueueService();

    private final DefaultTableModel tableModel =
            new DefaultTableModel(new String[]{
                    "Queue ID", "Vehicle ID", "Vehicle Model & Reg",
                    "Start Date", "End Date", "Duration (Hrs)",
                    "Est Cost (₹)", "Your Position", "Status", "Requested On"
            }, 0) {
                @Override
                public boolean isCellEditable(int row, int col) {
                    return false;
                }
            };

    private final JTable table = new JTable(tableModel);

    public CustomerQueuePanel(Customer customer) {
        this.customer = customer;

        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout(8, 4));
        JLabel title = new JLabel("My Waitlist & Rental Queue");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        JLabel subtitle = new JLabel(
                "Track your priority position in the queue. If the first renter cancels their booking, the vehicle is automatically assigned to you!"
        );
        subtitle.setForeground(Color.GRAY);
        headerPanel.add(title, BorderLayout.NORTH);
        headerPanel.add(subtitle, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        table.setRowHeight(25);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Custom renderer for Position (Column 7)
        table.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                if (value != null) {
                    int pos = Integer.parseInt(value.toString());
                    String status = tbl.getValueAt(row, 8).toString();
                    if ("WAITING".equalsIgnoreCase(status)) {
                        if (pos == 1) {
                            c.setForeground(new Color(211, 84, 0));
                            setText("★ Position #1 (Next in Line!)");
                        } else {
                            c.setForeground(new Color(175, 96, 26));
                            setText("Position #" + pos);
                        }
                    } else if ("PROMOTED".equalsIgnoreCase(status)) {
                        c.setForeground(new Color(39, 174, 96));
                        setText("Allocated (Booked)");
                    } else {
                        setText("-");
                    }
                }
                return c;
            }
        });

        // Custom renderer for Status (Column 8)
        table.getColumnModel().getColumn(8).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
                if (value != null) {
                    String str = value.toString();
                    if ("WAITING".equalsIgnoreCase(str)) {
                        c.setForeground(new Color(211, 84, 0));
                        setText("WAITING IN QUEUE");
                    } else if ("PROMOTED".equalsIgnoreCase(str)) {
                        c.setForeground(new Color(39, 174, 96));
                        setText("PROMOTED TO BOOKING ✓");
                    } else if ("CANCELLED".equalsIgnoreCase(str)) {
                        c.setForeground(new Color(127, 140, 141));
                        setText("CANCELLED");
                    }
                }
                return c;
            }
        });

        // Controls
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        JButton refreshBtn = new JButton("Refresh Queue");
        JButton cancelBtn = new JButton("Cancel My Waitlist Entry");
        JButton detailsBtn = new JButton("View Waitlist Details");

        controls.add(refreshBtn);
        controls.add(cancelBtn);
        controls.add(detailsBtn);

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.add(controls, BorderLayout.NORTH);
        content.add(new JScrollPane(table), BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);

        refreshBtn.addActionListener(e -> loadQueue());
        cancelBtn.addActionListener(e -> cancelQueueEntry());
        detailsBtn.addActionListener(e -> viewDetails());

        loadQueue();
    }

    public void loadQueue() {
        try {
            List<RentalQueueEntry> entries = queueService.getQueueByCustomerId(customer.getCustomerId());
            tableModel.setRowCount(0);

            for (RentalQueueEntry q : entries) {
                tableModel.addRow(new Object[]{
                        q.getQueueId(),
                        q.getVehicleId(),
                        q.getVehicleModel() + " (" + q.getVehicleRegistration() + ")",
                        q.getStartDate(),
                        q.getEndDate(),
                        q.getDurationHours(),
                        String.format("%.2f", q.getEstimatedAmount()),
                        q.getQueuePosition(),
                        q.getStatus(),
                        q.getCreatedAt() != null ? q.getCreatedAt().toString() : "N/A"
                });
            }
        } catch (SQLException ex) {
            error(ex);
        }
    }

    private void cancelQueueEntry() {
        int row = table.getSelectedRow();
        if (row < 0) {
            message("Select a waitlist entry from the table first.");
            return;
        }

        int qId = Integer.parseInt(tableModel.getValueAt(row, 0).toString());
        String status = tableModel.getValueAt(row, 8).toString();
        String vehicleInfo = tableModel.getValueAt(row, 2).toString();

        if ("CANCELLED".equalsIgnoreCase(status)) {
            message("This waitlist entry is already cancelled.");
            return;
        }

        if ("PROMOTED".equalsIgnoreCase(status)) {
            message("This waitlist entry has already been promoted to an active booking!\n" +
                    "To cancel the rental, please visit 'My Bookings'.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to leave the waiting list for:\n" +
                vehicleInfo + " (Queue #" + qId + ")?",
                "Confirm Leaving Waiting List",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            if (queueService.cancelQueueEntry(qId)) {
                message("You have left the waiting list for " + vehicleInfo + ".");
                loadQueue();
            } else {
                message("Failed to cancel waitlist entry.");
            }
        } catch (Exception ex) {
            error(ex);
        }
    }

    private void viewDetails() {
        int row = table.getSelectedRow();
        if (row < 0) {
            message("Select an entry from the table first.");
            return;
        }

        int qId = Integer.parseInt(tableModel.getValueAt(row, 0).toString());
        try {
            RentalQueueEntry q = queueService.getQueueEntryById(qId);
            if (q == null) {
                message("Queue entry not found.");
                return;
            }

            String posText = "Position #" + q.getQueuePosition();
            if ("WAITING".equalsIgnoreCase(q.getStatus()) && q.getQueuePosition() == 1) {
                posText += " (★ Next in Line - will receive vehicle if current booking cancels!)";
            }

            JOptionPane.showMessageDialog(
                    this,
                    "Waitlist Entry Summary:\n\n" +
                    "Queue ID: #" + q.getQueueId() + "\n" +
                    "Vehicle: " + q.getVehicleModel() + " (" + q.getVehicleRegistration() + ")\n" +
                    "Current Status: " + q.getStatus() + "\n" +
                    "Your Priority: " + posText + "\n\n" +
                    "Requested Rental Dates: " + q.getStartDate() + " to " + q.getEndDate() + "\n" +
                    "Duration: " + q.getDurationHours() + " hour(s)\n" +
                    "Estimated Cost: ₹" + String.format("%.2f", q.getEstimatedAmount()) + "\n" +
                    "Waitlist Entry Created: " + q.getCreatedAt() + "\n",
                    "Queue Details",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (SQLException ex) {
            error(ex);
        }
    }

    private void message(String text) {
        JOptionPane.showMessageDialog(this, text);
    }

    private void error(Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
