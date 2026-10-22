package com.resolvr.extension.dao;

import com.resolvr.dao.DatabaseConnection;
import com.resolvr.extension.model.MaintenanceRecord;
import com.resolvr.model.Vehicle;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MaintenanceDAO {

    public MaintenanceDAO() {
        initTable();
    }

    // Ensure the vehicle_maintenance table exists
    public void initTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS vehicle_maintenance (
                maintenance_id INT AUTO_INCREMENT PRIMARY KEY,
                vehicle_id INT NOT NULL,
                maintenance_type VARCHAR(50) NOT NULL,
                description TEXT NOT NULL,
                cost DECIMAL(10,2) NOT NULL DEFAULT 0.00,
                start_date DATE NOT NULL,
                end_date DATE,
                status VARCHAR(30) NOT NULL DEFAULT 'UNDER_MAINTENANCE',
                service_center VARCHAR(150),
                condition_notes TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id)
            )
        """;

        try (Connection con = DatabaseConnection.getConnection();
             Statement stmt = con.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Note: could not verify or create vehicle_maintenance table: " + e.getMessage());
        }
    }

    // Add a maintenance / repair record
    public boolean addMaintenance(MaintenanceRecord record) throws SQLException {
        String sql = """
            INSERT INTO vehicle_maintenance
            (vehicle_id, maintenance_type, description, cost, start_date, end_date, status, service_center, condition_notes)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, record.getVehicleId());
            ps.setString(2, record.getMaintenanceType());
            ps.setString(3, record.getDescription());
            ps.setDouble(4, record.getCost());
            ps.setDate(5, record.getStartDate());
            ps.setDate(6, record.getEndDate());
            ps.setString(7, record.getStatus());
            ps.setString(8, record.getServiceCenter());
            ps.setString(9, record.getConditionNotes());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        record.setMaintenanceId(rs.getInt(1));
                    }
                }
                return true;
            }
            return false;
        }
    }

    // Retrieve all maintenance records with vehicle registration and model
    public List<MaintenanceRecord> getAllMaintenance() throws SQLException {
        List<MaintenanceRecord> records = new ArrayList<>();
        String sql = """
            SELECT m.maintenance_id, m.vehicle_id, v.registration_no, v.model,
                   m.maintenance_type, m.description, m.cost, m.start_date,
                   m.end_date, m.status, m.service_center, m.condition_notes
            FROM vehicle_maintenance m
            JOIN vehicles v ON m.vehicle_id = v.vehicle_id
            ORDER BY m.maintenance_id DESC
        """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                records.add(mapRecord(rs));
            }
        }
        return records;
    }

    // Retrieve maintenance record by ID
    public MaintenanceRecord getMaintenanceById(int maintenanceId) throws SQLException {
        String sql = """
            SELECT m.maintenance_id, m.vehicle_id, v.registration_no, v.model,
                   m.maintenance_type, m.description, m.cost, m.start_date,
                   m.end_date, m.status, m.service_center, m.condition_notes
            FROM vehicle_maintenance m
            JOIN vehicles v ON m.vehicle_id = v.vehicle_id
            WHERE m.maintenance_id = ?
        """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, maintenanceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRecord(rs);
                }
            }
        }
        return null;
    }

    // Retrieve maintenance records for a vehicle
    public List<MaintenanceRecord> getMaintenanceByVehicleId(int vehicleId) throws SQLException {
        List<MaintenanceRecord> records = new ArrayList<>();
        String sql = """
            SELECT m.maintenance_id, m.vehicle_id, v.registration_no, v.model,
                   m.maintenance_type, m.description, m.cost, m.start_date,
                   m.end_date, m.status, m.service_center, m.condition_notes
            FROM vehicle_maintenance m
            JOIN vehicles v ON m.vehicle_id = v.vehicle_id
            WHERE m.vehicle_id = ?
            ORDER BY m.maintenance_id DESC
        """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, vehicleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(mapRecord(rs));
                }
            }
        }
        return records;
    }

    // Retrieve active maintenance records (under maintenance)
    public List<MaintenanceRecord> getActiveMaintenance() throws SQLException {
        List<MaintenanceRecord> records = new ArrayList<>();
        String sql = """
            SELECT m.maintenance_id, m.vehicle_id, v.registration_no, v.model,
                   m.maintenance_type, m.description, m.cost, m.start_date,
                   m.end_date, m.status, m.service_center, m.condition_notes
            FROM vehicle_maintenance m
            JOIN vehicles v ON m.vehicle_id = v.vehicle_id
            WHERE m.status = 'UNDER_MAINTENANCE'
            ORDER BY m.start_date ASC
        """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                records.add(mapRecord(rs));
            }
        }
        return records;
    }

    // Retrieve vehicles currently unavailable due to maintenance
    public List<Vehicle> getUnavailableVehiclesDueToMaintenance() throws SQLException {
        List<Vehicle> vehicles = new ArrayList<>();
        String sql = """
            SELECT vehicle_id, registration_no, model, category, price_per_hour, status
            FROM vehicles
            WHERE status = 'MAINTENANCE'
            ORDER BY vehicle_id ASC
        """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                vehicles.add(new Vehicle(
                    rs.getInt("vehicle_id"),
                    rs.getString("registration_no"),
                    rs.getString("model"),
                    rs.getString("category"),
                    rs.getDouble("price_per_hour"),
                    rs.getString("status")
                ));
            }
        }
        return vehicles;
    }

    // Retrieve latest return condition / damage details for a vehicle
    public String getLatestReturnConditionForVehicle(int vehicleId) throws SQLException {
        String sql = """
            SELECT ret.condition_notes, ret.damage_charge, ret.actual_return_date
            FROM `returns` ret
            JOIN rentals r ON ret.rental_id = r.rental_id
            WHERE r.vehicle_id = ?
            ORDER BY ret.return_id DESC
            LIMIT 1
        """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, vehicleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String notes = rs.getString("condition_notes");
                    double damage = rs.getDouble("damage_charge");
                    Date date = rs.getDate("actual_return_date");
                    StringBuilder sb = new StringBuilder();
                    if (notes != null && !notes.trim().isEmpty()) {
                        sb.append(notes.trim());
                    }
                    if (damage > 0) {
                        if (sb.length() > 0) sb.append(" | ");
                        sb.append(String.format("Reported Damage: ₹%.2f", damage));
                    }
                    if (date != null) {
                        if (sb.length() > 0) sb.append(" ");
                        sb.append("(Last Return: ").append(date).append(")");
                    }
                    return sb.toString();
                }
            }
        }
        return "";
    }

    // Check if a vehicle has any ongoing active maintenance
    public boolean hasActiveMaintenance(int vehicleId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM vehicle_maintenance WHERE vehicle_id = ? AND status = 'UNDER_MAINTENANCE'";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, vehicleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    // Update maintenance record
    public boolean updateMaintenance(MaintenanceRecord record) throws SQLException {
        String sql = """
            UPDATE vehicle_maintenance
            SET maintenance_type = ?, description = ?, cost = ?,
                start_date = ?, end_date = ?, status = ?,
                service_center = ?, condition_notes = ?
            WHERE maintenance_id = ?
        """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, record.getMaintenanceType());
            ps.setString(2, record.getDescription());
            ps.setDouble(3, record.getCost());
            ps.setDate(4, record.getStartDate());
            ps.setDate(5, record.getEndDate());
            ps.setString(6, record.getStatus());
            ps.setString(7, record.getServiceCenter());
            ps.setString(8, record.getConditionNotes());
            ps.setInt(9, record.getMaintenanceId());

            return ps.executeUpdate() > 0;
        }
    }

    // Complete maintenance
    public boolean completeMaintenance(int maintenanceId, Date endDate, double cost, String completionNotes) throws SQLException {
        String sql = """
            UPDATE vehicle_maintenance
            SET status = 'COMPLETED', end_date = ?, cost = ?,
                condition_notes = CASE
                    WHEN condition_notes IS NULL OR condition_notes = '' THEN ?
                    ELSE CONCAT(condition_notes, ' | Repaired: ', ?)
                END
            WHERE maintenance_id = ?
        """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setDate(1, endDate);
            ps.setDouble(2, cost);
            ps.setString(3, completionNotes);
            ps.setString(4, completionNotes);
            ps.setInt(5, maintenanceId);

            return ps.executeUpdate() > 0;
        }
    }

    // Delete a maintenance record
    public boolean deleteMaintenance(int maintenanceId) throws SQLException {
        String sql = "DELETE FROM vehicle_maintenance WHERE maintenance_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, maintenanceId);
            return ps.executeUpdate() > 0;
        }
    }

    private MaintenanceRecord mapRecord(ResultSet rs) throws SQLException {
        return new MaintenanceRecord(
            rs.getInt("maintenance_id"),
            rs.getInt("vehicle_id"),
            rs.getString("registration_no"),
            rs.getString("model"),
            rs.getString("maintenance_type"),
            rs.getString("description"),
            rs.getDouble("cost"),
            rs.getDate("start_date"),
            rs.getDate("end_date"),
            rs.getString("status"),
            rs.getString("service_center"),
            rs.getString("condition_notes")
        );
    }
}
