# MFA Security System

This is my engineering project. It is a Spring Boot backend combined with ESP32 hardware.

## Key Features

- Hardware integration (RFID/Fingerprint via ESP32).
- Dynamic QR Code generation and scanning.
- Real-time access logs using WebSockets.
- Role-Based Access Control (Admin, Team Leader, Staff).
- Location tracking (Entrance A, Document Warehouse).

## How to run

You can start the project using Docker or Maven.

### Docker Compose

You need to have Docker and Docker Compose installed on your computer. Open your terminal in the main project directory and run:

docker compose up --build

This starts the backend server and the MySQL database. You can open the admin panel in your browser at http://localhost:8080.

The database details are:
- Port: 3306
- User: mfa_user
- Password: mfa_pass

### Maven

If you want to run the code without Docker for the backend, you can use Maven. Make sure your database is running first. Use these commands:

mvn clean install
mvn spring-boot:run