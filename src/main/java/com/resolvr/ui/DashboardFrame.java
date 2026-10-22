package com.resolvr.ui;

import com.resolvr.extension.ui.MaintenancePanel;

import javax.swing.*;
import java.awt.*;

public class DashboardFrame extends JFrame {

    public DashboardFrame() {
        setTitle("RESOLVR - Rental Car Management");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1150, 760);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);

        JPanel header = new JPanel(new BorderLayout(10, 10));
        header.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        header.setBackground(new Color(29, 42, 65));

        JLabel brand = new JLabel("RESOLVR");
        brand.setForeground(Color.WHITE);
        brand.setFont(new Font("SansSerif", Font.BOLD, 25));

        JLabel subtitle = new JLabel("Rental Car Management System");
        subtitle.setForeground(new Color(220, 226, 235));
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JPanel brandPanel = new JPanel(new GridLayout(2, 1, 0, 3));
        brandPanel.setOpaque(false);
        brandPanel.add(brand);
        brandPanel.add(subtitle);

        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });

        header.add(brandPanel, BorderLayout.WEST);
        header.add(logout, BorderLayout.EAST);

        JTabbedPane tabs = new JTabbedPane();
        VehiclePanel vehiclePanel = new VehiclePanel();
        CustomerPanel customerPanel = new CustomerPanel();
        RentalPanel rentalPanel = new RentalPanel();
        PaymentPanel paymentPanel = new PaymentPanel();
        ReturnPanel returnPanel = new ReturnPanel();
        MaintenancePanel maintenancePanel = new MaintenancePanel();

        tabs.addTab("Vehicles", vehiclePanel);
        tabs.addTab("Customers", customerPanel);
        tabs.addTab("Rentals", rentalPanel);
        tabs.addTab("Payments", paymentPanel);
        tabs.addTab("Returns", returnPanel);
        tabs.addTab("Maintenance", maintenancePanel);

        // Auto refresh relevant tab contents on switch
        tabs.addChangeListener(e -> {
            int selectedIndex = tabs.getSelectedIndex();
            if (selectedIndex == 0) {
                vehiclePanel.loadVehicles();
            } else if (selectedIndex == 4) {
                returnPanel.loadReturns();
            } else if (selectedIndex == 5) {
                maintenancePanel.loadVehiclesDropdown();
                maintenancePanel.loadRecords();
                maintenancePanel.updateUnavailableBanner();
            }
        });

        setLayout(new BorderLayout());
        add(header, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
    }
}
