package com.resolvr.extension.dao;

import com.resolvr.dao.DatabaseConnection;
import com.resolvr.extension.model.RentalQueueEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class RentalQueueDAO {

    public RentalQueueDAO() {
        initTable();
    }

    public void initTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS rental_queue (
                queue_id INT AUTO_INCREMENT PRIMARY KEY,
                vehicle_id INT NOT NULL,
                customer_id INT NOT NULL,
                start_date DATE NOT NULL,
                end_date DATE NOT NULL,
                duration_hours INT NOT NULL DEFAULT 4,
                estimated_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
                queue_position INT NOT NULL DEFAULT 1,
                status VARCHAR(30) NOT NULL DEFAULT 'WAITING',
                notes TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id),
                FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
            )
        """;

        try (Connection con = DatabaseConnection.getConnection();
             Statement stmt = con.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Note: could not verify or create rental_queue table: " + e.getMessage());
        }
    }

    // Add customer to queue with auto-computed next FIFO queue_position
    public boolean addToQueue(RentalQueueEntry entry) throws SQLException {
        String posSql = """
            SELECT COALESCE(MAX(queue_position), 0) + 1
            FROM rental_queue
            WHERE vehicle_id = ? AND status = 'WAITING'
        """;

        int nextPos = 1;
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement psPos = con.prepareStatement(posSql)) {
            psPos.setInt(1, entry.getVehicleId());
            try (ResultSet rs = psPos.executeQuery()) {
                if (rs.next()) {
                    nextPos = rs.getInt(1);
                }
            }
        }
        entry.setQueuePosition(nextPos);

        String insertSql = """
            INSERT INTO rental_queue
            (vehicle_id, customer_id, start_date, end_date, duration_hours, estimated_amount, queue_position, status, notes)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, entry.getVehicleId());
            ps.setInt(2, entry.getCustomerId());
            ps.setDate(3, entry.getStartDate());
            ps.setDate(4, entry.getEndDate());
            ps.setInt(5, entry.getDurationHours());
            ps.setDouble(6, entry.getEstimatedAmount());
            ps.setInt(7, entry.getQueuePosition());
            ps.setString(8, entry.getStatus() != null ? entry.getStatus() : "WAITING");
            ps.setString(9, entry.getNotes());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        entry.setQueueId(rs.getInt(1));
                    }
                }
                return true;
            }
            return false;
        }
    }

    // Check if customer already has an active WAITING entry for this vehicle
    public boolean isCustomerInQueue(int vehicleId, int customerId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM rental_queue WHERE vehicle_id = ? AND customer_id = ? AND status = 'WAITING'";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, vehicleId);
            ps.setInt(2, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    // Get the next customer waiting in line (FIFO: lowest position, earliest created_at)
    public RentalQueueEntry getFirstWaitingInQueue(int vehicleId) throws SQLException {
        String sql = """
            SELECT q.queue_id, q.vehicle_id, v.registration_no, v.model,
                   q.customer_id, c.name, c.phone,
                   q.start_date, q.end_date, q.duration_hours, q.estimated_amount,
                   q.queue_position, q.status, q.notes, q.created_at
            FROM rental_queue q
            JOIN vehicles v ON q.vehicle_id = v.vehicle_id
            JOIN customers c ON q.customer_id = c.customer_id
            WHERE q.vehicle_id = ? AND q.status = 'WAITING'
            ORDER BY q.queue_position ASC, q.created_at ASC
            LIMIT 1
        """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, vehicleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRecord(rs);
                }
            }
        }
        return null;
    }

    // Retrieve all queue entries with vehicle and customer details
    public List<RentalQueueEntry> getAllQueueEntries() throws SQLException {
        List<RentalQueueEntry> list = new ArrayList<>();
        String sql = """
            SELECT q.queue_id, q.vehicle_id, v.registration_no, v.model,
                   q.customer_id, c.name, c.phone,
                   q.start_date, q.end_date, q.duration_hours, q.estimated_amount,
                   q.queue_position, q.status, q.notes, q.created_at
            FROM rental_queue q
            JOIN vehicles v ON q.vehicle_id = v.vehicle_id
            JOIN customers c ON q.customer_id = c.customer_id
            ORDER BY CASE WHEN q.status = 'WAITING' THEN 0 ELSE 1 END,
                     q.vehicle_id ASC,
                     q.queue_position ASC,
                     q.created_at DESC
        """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRecord(rs));
            }
        }
        return list;
    }

    // Retrieve active waiting entries for a specific vehicle
    public List<RentalQueueEntry> getQueueForVehicle(int vehicleId) throws SQLException {
        List<RentalQueueEntry> list = new ArrayList<>();
        String sql = """
            SELECT q.queue_id, q.vehicle_id, v.registration_no, v.model,
                   q.customer_id, c.name, c.phone,
                   q.start_date, q.end_date, q.duration_hours, q.estimated_amount,
                   q.queue_position, q.status, q.notes, q.created_at
            FROM rental_queue q
            JOIN vehicles v ON q.vehicle_id = v.vehicle_id
            JOIN customers c ON q.customer_id = c.customer_id
            WHERE q.vehicle_id = ? AND q.status = 'WAITING'
            ORDER BY q.queue_position ASC, q.created_at ASC
        """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, vehicleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRecord(rs));
                }
            }
        }
        return list;
    }

    // Retrieve all queue entries for a customer
    public List<RentalQueueEntry> getQueueByCustomerId(int customerId) throws SQLException {
        List<RentalQueueEntry> list = new ArrayList<>();
        String sql = """
            SELECT q.queue_id, q.vehicle_id, v.registration_no, v.model,
                   q.customer_id, c.name, c.phone,
                   q.start_date, q.end_date, q.duration_hours, q.estimated_amount,
                   q.queue_position, q.status, q.notes, q.created_at
            FROM rental_queue q
            JOIN vehicles v ON q.vehicle_id = v.vehicle_id
            JOIN customers c ON q.customer_id = c.customer_id
            WHERE q.customer_id = ?
            ORDER BY q.queue_id DESC
        """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRecord(rs));
                }
            }
        }
        return list;
    }

    // Retrieve single queue entry by ID
    public RentalQueueEntry getQueueEntryById(int queueId) throws SQLException {
        String sql = """
            SELECT q.queue_id, q.vehicle_id, v.registration_no, v.model,
                   q.customer_id, c.name, c.phone,
                   q.start_date, q.end_date, q.duration_hours, q.estimated_amount,
                   q.queue_position, q.status, q.notes, q.created_at
            FROM rental_queue q
            JOIN vehicles v ON q.vehicle_id = v.vehicle_id
            JOIN customers c ON q.customer_id = c.customer_id
            WHERE q.queue_id = ?
        """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, queueId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRecord(rs);
                }
            }
        }
        return null;
    }

    // Update queue status (e.g. PROMOTED, CANCELLED)
    public boolean updateQueueStatus(int queueId, String status) throws SQLException {
        String sql = "UPDATE rental_queue SET status = ? WHERE queue_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, queueId);
            return ps.executeUpdate() > 0;
        }
    }

    // Recalculate and re-index contiguous positions for all active WAITING entries of a vehicle
    public void recalculatePositions(int vehicleId) throws SQLException {
        String selectSql = """
            SELECT queue_id
            FROM rental_queue
            WHERE vehicle_id = ? AND status = 'WAITING'
            ORDER BY queue_position ASC, created_at ASC
        """;

        List<Integer> ids = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(selectSql)) {
            ps.setInt(1, vehicleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt(1));
                }
            }
        }

        String updateSql = "UPDATE rental_queue SET queue_position = ? WHERE queue_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(updateSql)) {
            for (int i = 0; i < ids.size(); i++) {
                ps.setInt(1, i + 1);
                ps.setInt(2, ids.get(i));
                ps.executeUpdate();
            }
        }
    }

    // Count of active waiting entries
    public int getActiveQueueCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM rental_queue WHERE status = 'WAITING'";
        try (Connection con = DatabaseConnection.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    // Count of distinct vehicles with waiting customers
    public int getActiveVehiclesInQueueCount() throws SQLException {
        String sql = "SELECT COUNT(DISTINCT vehicle_id) FROM rental_queue WHERE status = 'WAITING'";
        try (Connection con = DatabaseConnection.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    // Delete record (useful for tests and cleanup)
    public boolean deleteQueueEntry(int queueId) throws SQLException {
        String sql = "DELETE FROM rental_queue WHERE queue_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, queueId);
            return ps.executeUpdate() > 0;
        }
    }

    private RentalQueueEntry mapRecord(ResultSet rs) throws SQLException {
        return new RentalQueueEntry(
                rs.getInt("queue_id"),
                rs.getInt("vehicle_id"),
                rs.getString("registration_no"),
                rs.getString("model"),
                rs.getInt("customer_id"),
                rs.getString("name"),
                rs.getString("phone"),
                rs.getDate("start_date"),
                rs.getDate("end_date"),
                rs.getInt("duration_hours"),
                rs.getDouble("estimated_amount"),
                rs.getInt("queue_position"),
                rs.getString("status"),
                rs.getString("notes"),
                rs.getTimestamp("created_at")
        );
    }
}
