CREATE DATABASE IF NOT EXISTS resolvr;
USE resolvr;

CREATE TABLE IF NOT EXISTS vehicles (
    vehicle_id INT AUTO_INCREMENT PRIMARY KEY,
    registration_no VARCHAR(30) NOT NULL UNIQUE,
    model VARCHAR(100) NOT NULL,
    category VARCHAR(30) NOT NULL,
    price_per_hour DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE'
);

CREATE TABLE IF NOT EXISTS customers (
    customer_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100),
    license_no VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS rentals (
    rental_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    vehicle_id INT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'BOOKED',

    FOREIGN KEY (customer_id)
        REFERENCES customers(customer_id),

    FOREIGN KEY (vehicle_id)
        REFERENCES vehicles(vehicle_id)
);

CREATE TABLE IF NOT EXISTS payments (
    payment_id INT AUTO_INCREMENT PRIMARY KEY,
    rental_id INT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    method VARCHAR(30) NOT NULL,
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (rental_id)
        REFERENCES rentals(rental_id)
);

CREATE TABLE IF NOT EXISTS `returns` (
    return_id INT AUTO_INCREMENT PRIMARY KEY,
    rental_id INT NOT NULL UNIQUE,
    actual_return_date DATE NOT NULL,
    damage_charge DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    refund_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    condition_notes TEXT,

    FOREIGN KEY (rental_id)
        REFERENCES rentals(rental_id)
);

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

    FOREIGN KEY (vehicle_id)
        REFERENCES vehicles(vehicle_id)
);
