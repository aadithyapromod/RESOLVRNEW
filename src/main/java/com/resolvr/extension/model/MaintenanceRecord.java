package com.resolvr.extension.model;

import java.sql.Date;

public class MaintenanceRecord {

    private int maintenanceId;
    private int vehicleId;
    private String vehicleRegistration;
    private String vehicleModel;
    private String maintenanceType;
    private String description;
    private double cost;
    private Date startDate;
    private Date endDate;
    private String status;
    private String serviceCenter;
    private String conditionNotes;

    public MaintenanceRecord() {
    }

    public MaintenanceRecord(int maintenanceId, int vehicleId, String vehicleRegistration,
                             String vehicleModel, String maintenanceType, String description,
                             double cost, Date startDate, Date endDate,
                             String status, String serviceCenter, String conditionNotes) {
        this.maintenanceId = maintenanceId;
        this.vehicleId = vehicleId;
        this.vehicleRegistration = vehicleRegistration;
        this.vehicleModel = vehicleModel;
        this.maintenanceType = maintenanceType;
        this.description = description;
        this.cost = cost;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.serviceCenter = serviceCenter;
        this.conditionNotes = conditionNotes;
    }

    public MaintenanceRecord(int vehicleId, String maintenanceType, String description,
                             double cost, Date startDate, Date endDate,
                             String status, String serviceCenter, String conditionNotes) {
        this(0, vehicleId, null, null, maintenanceType, description,
             cost, startDate, endDate, status, serviceCenter, conditionNotes);
    }

    public int getMaintenanceId() {
        return maintenanceId;
    }

    public void setMaintenanceId(int maintenanceId) {
        this.maintenanceId = maintenanceId;
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

    public String getMaintenanceType() {
        return maintenanceType;
    }

    public void setMaintenanceType(String maintenanceType) {
        this.maintenanceType = maintenanceType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getServiceCenter() {
        return serviceCenter;
    }

    public void setServiceCenter(String serviceCenter) {
        this.serviceCenter = serviceCenter;
    }

    public String getConditionNotes() {
        return conditionNotes;
    }

    public void setConditionNotes(String conditionNotes) {
        this.conditionNotes = conditionNotes;
    }
}
