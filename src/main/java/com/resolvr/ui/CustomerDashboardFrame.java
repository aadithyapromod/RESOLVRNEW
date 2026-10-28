package com.resolvr.ui;

import com.resolvr.model.Customer;

import javax.swing.*;
import java.awt.*;

public class CustomerDashboardFrame extends JFrame {

    private final Customer customer;

    public CustomerDashboardFrame(Customer customer) {
        this.customer = customer;

        setTitle("RESOLVR - Customer Portal (" + customer.getName() + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1150, 760);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);

        // Top Navigation Header
        JPanel header = new JPanel(new BorderLayout(10, 10));
        header.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        header.setBackground(new Color(29, 42, 65));

        JLabel brand = new JLabel("RESOLVR");
        brand.setForeground(Color.WHITE);
        brand.setFont(new Font("SansSerif", Font.BOLD, 25));

        JLabel subtitle = new JLabel(
                "Customer Portal | Logged in as: " + customer.getName() +
                " (ID: #" + customer.getCustomerId() + " | Licence: " + customer.getLicenseNo() + ")"
        );
        subtitle.setForeground(new Color(220, 226, 235));
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JPanel brandPanel = new JPanel(new GridLayout(2, 1, 0, 3));
        brandPanel.setOpaque(false);
        brandPanel.add(brand);
        brandPanel.add(subtitle);

        JButton logout = new JButton("Logout");
        logout.setFocusPainted(false);
        logout.addActionListener(e -> {
            dispose();
            new CustomerLoginFrame().setVisible(true);
        });

        header.add(brandPanel, BorderLayout.WEST);
        header.add(logout, BorderLayout.EAST);

        // Tabs
        JTabbedPane tabs = new JTabbedPane();
        CustomerRentVehiclePanel rentPanel = new CustomerRentVehiclePanel(customer);
        CustomerBookingsPanel bookingsPanel = new CustomerBookingsPanel(customer);
        CustomerPaymentPanel paymentPanel = new CustomerPaymentPanel(customer);
        CustomerReturnPanel returnPanel = new CustomerReturnPanel(customer);
        CustomerQueuePanel queuePanel = new CustomerQueuePanel(customer);

        tabs.addTab("Browse & Rent Vehicles", rentPanel);
        tabs.addTab("My Bookings", bookingsPanel);
        tabs.addTab("My Payments", paymentPanel);
        tabs.addTab("Returns", returnPanel);
        tabs.addTab("My Queue / Waitlist", queuePanel);

        // When switching tabs, automatically refresh
        tabs.addChangeListener(e -> {
            int selectedIndex = tabs.getSelectedIndex();
            if (selectedIndex == 0) {
                rentPanel.loadAvailableVehicles();
            } else if (selectedIndex == 1) {
                bookingsPanel.loadBookings();
            } else if (selectedIndex == 2) {
                paymentPanel.loadPayments();
            } else if (selectedIndex == 3) {
                returnPanel.loadActiveRentals();
                returnPanel.loadReturnHistory();
            } else if (selectedIndex == 4) {
                queuePanel.loadQueue();
            }
        });

        setLayout(new BorderLayout());
        add(header, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
    }
}
