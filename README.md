# MFA System - Student Project (NFC + Fingerprint)

Two-stage authentication system requiring an NFC card and a fingerprint scan to grant access. Managed via a Spring Boot backend and a web-based admin panel.

## Tech Stack

* **Backend:** Java 17, Spring Boot 3
* **Database:** MySQL 8.0 (Dockerized)
* **Frontend:** Thymeleaf + WebSockets (Live Logs)
* **Hardware:** ESP32 (C++ / Arduino)
* **Containerization:** Docker + Docker Compose

## Hardware Connections (ESP32 Pinout)

* **LCD 16x2 (I2C):** VCC: 5V (VIN), GND: GND, SDA: 21, SCL: 22
* **AS608 Fingerprint:** VCC: 5V (VIN), GND: GND, TX: 26, RX: 27
* **RC522 RFID:** VCC: 3.3V, SCK: 18, MISO: 19, MOSI: 23, SS: 5, RST: 14
* **Passive Buzzer:** SIG: 13, GND: GND
* **Joystick:** VCC: 3.3V, VRx: 34, SW: 32

## How to Run

Requirement: Docker and Docker Compose installed.

```bash
docker compose up --build

```

**Services:**

* Admin Panel: http://localhost:8080
* MySQL: localhost:3306 (user: root, password: root)

## Core API Endpoints

### Card Validation

`GET /api/verify/card/{uid}`
Checks if the NFC card UID exists in the database before proceeding to fingerprint scan.

### MFA Verification

`POST /api/verify`
Validates if the provided NFC UID and Fingerprint ID belong to the same active employee.

### Enrollment Helper

`GET /api/enroll/next-available-id`
Returns the next free slot ID for the fingerprint sensor based on the highest ID in the database.

## Admin Features

* Add/Remove employees with automatic ID assignment.
* Real-time access logs via WebSockets.
* Toggle employee active status to block access without deleting records.

## Project Notes

* **Power Management:** Uses VIN (5V) for the sensor and LCD to ensure stability.
* **Audio Feedback:** Passive buzzer uses different frequencies for SUCCESS and ERROR states.
* **Database Reset:** Use `docker compose down -v` to clear all data and reset IDs to 1.