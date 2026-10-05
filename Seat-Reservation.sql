
CREATE DATABASE IF NOT EXISTS seat_reservation;
USE seat_reservation;

-- Shows Table
CREATE TABLE IF NOT EXISTS shows (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price_paise BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Seats Table
CREATE TABLE IF NOT EXISTS seats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    show_id VARCHAR(64) NOT NULL,
    seat_number VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'AVAILABLE', -- AVAILABLE, CONFIRMED
    reservation_id VARCHAR(64),
    version BIGINT DEFAULT 0,
    CONSTRAINT uk_show_seat UNIQUE (show_id, seat_number),
    CONSTRAINT fk_seats_show FOREIGN KEY (show_id) REFERENCES shows(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_seats_show_status ON seats(show_id, status);

-- Reservations Table
CREATE TABLE IF NOT EXISTS reservations (
    id VARCHAR(64) PRIMARY KEY,
    show_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    amount_paise BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL, -- CONFIRMED, CANCELLED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reservations_show FOREIGN KEY (show_id) REFERENCES shows(id)
) ENGINE=InnoDB;

CREATE INDEX idx_reservations_show_user ON reservations(show_id, user_id);

-- Idempotency Keys Table
CREATE TABLE IF NOT EXISTS idempotency_keys (
    idempotency_key VARCHAR(128) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    reservation_id VARCHAR(64) NOT NULL,
    response_json TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;