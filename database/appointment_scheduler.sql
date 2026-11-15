-- =====================================================================
-- CST-323 CLC Project - Group A2
-- Doctor's Office Appointment Scheduling Application
-- DDL + seed data for MySQL 8.x
--
-- Run in MySQL Workbench (File > Run SQL Script) or from a terminal:
--     mysql -u root -p < database/appointment_scheduler.sql
--
-- WARNING: drops and re-creates the appointment_scheduler database.
-- Test data only - never load real patient information.
-- =====================================================================

-- MySQL Workbench turns on safe update mode by default; the last UPDATE needs it off.
SET SQL_SAFE_UPDATES = 0;

DROP DATABASE IF EXISTS appointment_scheduler;
CREATE DATABASE appointment_scheduler
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
USE appointment_scheduler;

-- ---------------------------------------------------------------------
-- users: patients and office staff (admins)
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    first_name     VARCHAR(50)  NOT NULL,
    last_name      VARCHAR(50)  NOT NULL,
    email          VARCHAR(100) NOT NULL,
    phone          VARCHAR(20)  NULL,
    date_of_birth  DATE         NULL,
    password_hash  VARCHAR(100) NOT NULL,             -- BCrypt hash, never plain text
    role           VARCHAR(20)  NOT NULL DEFAULT 'PATIENT',
    created_at     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('PATIENT', 'ADMIN'))
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- services: visit types a patient can book (VisitType entity)
-- ---------------------------------------------------------------------
CREATE TABLE services (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    name              VARCHAR(100) NOT NULL,
    description       VARCHAR(255) NULL,
    duration_minutes  INT          NOT NULL DEFAULT 30,
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_services PRIMARY KEY (id),
    CONSTRAINT uk_services_name UNIQUE (name),
    CONSTRAINT ck_services_duration CHECK (duration_minutes BETWEEN 10 AND 240)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- time_slots: the doctor's bookable blocks of time
-- ---------------------------------------------------------------------
CREATE TABLE time_slots (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    slot_date   DATE     NOT NULL,
    start_time  TIME     NOT NULL,
    end_time    TIME     NOT NULL,
    available   BOOLEAN  NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_time_slots PRIMARY KEY (id),
    CONSTRAINT uk_slot_date_start UNIQUE (slot_date, start_time),
    CONSTRAINT ck_slot_times CHECK (end_time > start_time)
) ENGINE = InnoDB;

CREATE INDEX ix_time_slots_date_available ON time_slots (slot_date, available);

-- ---------------------------------------------------------------------
-- appointments: one patient + one visit type + one time slot
-- UNIQUE (slot_id) is what prevents double booking at the database level.
-- ---------------------------------------------------------------------
CREATE TABLE appointments (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    service_id  BIGINT       NOT NULL,
    slot_id     BIGINT       NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'BOOKED',
    notes       VARCHAR(255) NULL,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)  NULL,
    CONSTRAINT pk_appointments PRIMARY KEY (id),
    CONSTRAINT uk_appointments_slot UNIQUE (slot_id),
    CONSTRAINT fk_appointments_user    FOREIGN KEY (user_id)    REFERENCES users (id),
    CONSTRAINT fk_appointments_service FOREIGN KEY (service_id) REFERENCES services (id),
    CONSTRAINT fk_appointments_slot    FOREIGN KEY (slot_id)    REFERENCES time_slots (id),
    CONSTRAINT ck_appointments_status CHECK (status IN ('BOOKED', 'COMPLETED'))
) ENGINE = InnoDB;

CREATE INDEX ix_appointments_user ON appointments (user_id);

-- =====================================================================
-- Seed data (test data only)
-- =====================================================================

-- Accounts. Passwords are BCrypt hashes of:
--   admin@clinic.test   / Admin123!
--   patient@clinic.test / Patient123!
INSERT INTO users (first_name, last_name, email, phone, date_of_birth, password_hash, role) VALUES
    ('Office', 'Admin', 'admin@clinic.test',   '602-555-0100', NULL,
     '$2a$10$MOmlpQv/gkgbzLsQKwt26.FFZf1765EDJa9HcqsZm.ijnHOsipFfO', 'ADMIN'),
    ('Test',   'Patient', 'patient@clinic.test', '602-555-0123', '1990-05-14',
     '$2a$10$HlfMtrrIEM.8MZ6R.qnTm.oMe8eDmjP8qhRfkCgPhByB0yeEZkTYS', 'PATIENT');

INSERT INTO services (name, description, duration_minutes, active) VALUES
    ('Annual Checkup', 'Yearly physical exam and preventive care review.', 30, TRUE),
    ('Sick Visit',     'Same-week visit for illness or new symptoms.',     30, TRUE),
    ('Follow-up',      'Check-in after a previous visit or new treatment.', 30, TRUE),
    ('Vaccination',    'Flu shot or other routine immunization.',           30, TRUE);

-- Open 30-minute slots, 9:00 AM - 5:00 PM, on weekdays for the next 3 weeks
-- (dates are relative to the day the script is run).
INSERT INTO time_slots (slot_date, start_time, end_time, available)
SELECT days.d, times.t, ADDTIME(times.t, '00:30:00'), TRUE
FROM (
        SELECT CURDATE() + INTERVAL n DAY AS d
        FROM (SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10 UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15 UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL SELECT 20) day_numbers
     ) days
CROSS JOIN (
        SELECT CAST('09:00:00' AS TIME) AS t
        UNION ALL SELECT CAST('09:30:00' AS TIME)
        UNION ALL SELECT CAST('10:00:00' AS TIME)
        UNION ALL SELECT CAST('10:30:00' AS TIME)
        UNION ALL SELECT CAST('11:00:00' AS TIME)
        UNION ALL SELECT CAST('11:30:00' AS TIME)
        UNION ALL SELECT CAST('12:00:00' AS TIME)
        UNION ALL SELECT CAST('12:30:00' AS TIME)
        UNION ALL SELECT CAST('13:00:00' AS TIME)
        UNION ALL SELECT CAST('13:30:00' AS TIME)
        UNION ALL SELECT CAST('14:00:00' AS TIME)
        UNION ALL SELECT CAST('14:30:00' AS TIME)
        UNION ALL SELECT CAST('15:00:00' AS TIME)
        UNION ALL SELECT CAST('15:30:00' AS TIME)
        UNION ALL SELECT CAST('16:00:00' AS TIME)
        UNION ALL SELECT CAST('16:30:00' AS TIME)
     ) times
WHERE DAYOFWEEK(days.d) BETWEEN 2 AND 6     -- Monday to Friday
ORDER BY days.d, times.t;

-- One sample booking for the test patient on the first open slot.
INSERT INTO appointments (user_id, service_id, slot_id, status, notes)
SELECT u.id, s.id, ts.id, 'BOOKED', 'Sample appointment from seed data'
FROM users u, services s,
     (SELECT id FROM time_slots ORDER BY slot_date, start_time LIMIT 1) ts
WHERE u.email = 'patient@clinic.test' AND s.name = 'Annual Checkup';

UPDATE time_slots SET available = FALSE
WHERE id IN (SELECT slot_id FROM (SELECT slot_id FROM appointments) booked);

SET SQL_SAFE_UPDATES = 1;
