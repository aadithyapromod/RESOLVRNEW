package com.resolvr.extension.model;

import java.sql.Date;
import java.sql.Timestamp;

public class RentalQueueEntry {

    private int queueId;
    private int vehicleId;
    private String vehicleRegistration;
    private String vehicleModel;
    private int customerId;
    private String customerName;
    private String customerPhone;
    private Date startDate;
    private Date endDate;
    private int durationHours;
    private double estimatedAmount;
    private int queuePosition;
    private String status;
    private String notes;
    private Timestamp createdAt;

    public RentalQueueEntry() {
    }

    public RentalQueueEntry(int queueId, int vehicleId, String vehicleRegistration, String vehicleModel,
                            int customerId, String customerName, String customerPhone,
                            Date startDate, Date endDate, int durationHours, double estimatedAmount,
                            int queuePosition, String status, String notes, Timestamp createdAt) {
        this.queueId = queueId;
        this.vehicleId = vehicleId;
        this.vehicleRegistration = vehicleRegistration;
        this.vehicleModel = vehicleModel;
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.startDate = startDate;
        this.endDate = endDate;
        this.durationHours = durationHours;
        this.estimatedAmount = estimatedAmount;
        this.queuePosition = queuePosition;
        this.status = status;
        this.notes = notes;
        this.createdAt = createdAt;
    }

    public RentalQueueEntry(int vehicleId, int customerId, Date startDate, Date endDate,
                            int durationHours, double estimatedAmount, String notes) {
        this(0, vehicleId, null, null, customerId, null, null,
             startDate, endDate, durationHours, estimatedAmount,
             1, "WAITING", notes, null);
    }

    public int getQueueId() {
        return queueId;
    }

    public void setQueueId(int queueId) {
        this.queueId = queueId;
    }

    public int getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(int vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getVehicleRegistration() {
        return vehicleRegistration;
    }

    public void setVehicleRegistration(String vehicleRegistration) {
        this.vehicleRegistration = vehicleRegistration;
    }

    public String getVehicleModel() {
        return vehicleModel;
    }

    public void setVehicleModel(String vehicleModel) {
        this.vehicleModel = vehicleModel;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public int getDurationHours() {
        return durationHours;
    }

    public void setDurationHours(int durationHours) {
        this.durationHours = durationHours;
    }

    public double getEstimatedAmount() {
        return estimatedAmount;
    }

    public void setEstimatedAmount(double estimatedAmount) {
        this.estimatedAmount = estimatedAmount;
    }

    public int getQueuePosition() {
        return queuePosition;
    }

    public void setQueuePosition(int queuePosition) {
        this.queuePosition = queuePosition;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
