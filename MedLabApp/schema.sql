-- Sante Diagnostics LIMS Database Schema
-- Target: PostgreSQL 

-- Drop tables if they exist to allow clean resets during testing


-- 1. USERS TABLE
-- Stores credentials and access states for all three roles
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL, -- To store BCrypt-hashed passwords 
    role VARCHAR(20) NOT NULL CHECK (role IN ('SUPER_ADMIN', 'LAB_ATTENDANT', 'CUSTOMER')), -- Enforces 3-tier hierarchy 
    is_verified BOOLEAN DEFAULT FALSE, -- Handles self-registration email verification 
    force_password_change BOOLEAN DEFAULT FALSE, -- Staff-created account flag 
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reset_code VARCHAR(10),
    reset_code_expiry TIMESTAMP
    
);

-- 2. TEST TYPES TABLE (Custom Test Builder)
-- Allows the Super Admin to configure available tests
CREATE TABLE test_types (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE, -- e.g., 'Blood Test', 'MRI Imaging' 
    price DECIMAL(10, 2) NOT NULL CHECK (price >= 0.00),
    tat_hours INT NOT NULL CHECK (tat_hours > 0), -- Standard Turnaround Time in hours 
    result_format VARCHAR(20) NOT NULL CHECK (result_format IN ('NUMERIC', 'TEXT', 'PDF', 'IMAGE')) -- Format configuration 
);

-- 3. TEST REQUESTS TABLE
-- Tracks orders, billing verification states, and deadlines
CREATE TABLE test_requests (
    id SERIAL PRIMARY KEY,
    customer_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE, 
    test_type_id INT NOT NULL REFERENCES test_types(id) ON DELETE RESTRICT,
    payment_status VARCHAR(20) DEFAULT 'UNPAID' CHECK (payment_status IN ('PAID', 'UNPAID')), 
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deadline_date TIMESTAMP NOT NULL, -- Computed at order placement using the test's tat_hours
    FOREIGN KEY (customer_id) REFERENCES users(id)
);

-- 4. SAMPLES & RESULTS LIFECYCLE TABLE
-- Manages processing states and tracks references to attached files
CREATE TABLE samples (
    id SERIAL PRIMARY KEY,
    test_request_id INT UNIQUE NOT NULL REFERENCES test_requests(id) ON DELETE CASCADE,
    status VARCHAR(30) DEFAULT 'COLLECTED' CHECK (status IN ('COLLECTED', 'PROCESSING', 'VALIDATED')), -- Lifecycle stages 
    pdf_report_path VARCHAR(512), -- File reference path for uploaded PDF reports 
    image_report_path VARCHAR(512), -- File reference path for medical images 
    is_verified BOOLEAN DEFAULT FALSE, -- Manual verification gate 
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 5. IMMUTABLE AUDIT TRAIL TABLE
-- Logs systemic operations securely. This log should only be appended to
CREATE TABLE audit_log (
    id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users(id) ON DELETE SET NULL, -- Tracks who executed the action 
    action_type VARCHAR(50) NOT NULL, -- e.g., 'LOGIN', 'RESULT_MODIFICATION', 'TEST_CREATED' 
    description TEXT NOT NULL, 
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Seed initial default Super Admin account (Password: Admin123! - run through BCrypt before deployment)
INSERT INTO users (name, email, password_hash, role, is_verified, force_password_change) 
VALUES ('System Admin', 'admin@sante.com', '$2a$12$gjL9S5Cxfq9lXP7Wt9lxW.Weg9VdNBiQcwBBWt4PzBDol.pRv4Es2', 'SUPER_ADMIN', TRUE, FALSE);


