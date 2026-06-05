# Multi-Factor Authentication System

This project is a physical access control system designed to manage entry into different facility zones. It uses an ESP32 to interface with hardware sensors and a Spring Boot backend to verify authentication requests.

## Wiring diagram

<img width="3718" height="1494" alt="mfa-1" src="https://github.com/user-attachments/assets/f528029c-eb50-4eeb-bcc5-cf9e5cb56149" />


### Hardware Components

**Main Board & Modules:**
* ESP32 Development Board (ESP32-WROOM-32 / DEVKITV1)
* MFRC522 RFID Reader Module
* AS608 Optical Fingerprint Scanner
* LCD Display with I2C Adapter
* KY-023 Analog Joystick Module
* Buzzer module

**Passive Components & Indicators:**
* 2x LEDs (1x Green, 1x Red)
* 2x 330Ω Resistors (Current limiting for LEDs)
* Decoupling Capacitors (Ceramic):
  * 5x 100nF
* Decoupling Capacitors (Electrolytic for power rail stability):
  * 1x 47μF
  * 1x 220μF
  * 2x 470μF
  * 1x 1000μF

## Software Stack
- Java and Spring Boot (Backend application)
- C++ (ESP32 hardware logic)
- Python (QR reading script)
- MySQL (Data storage)
- Docker (Environment setup)

## Features / Access Logic
The system enforces location-based access control based on user roles and multi-factor authentication.

- **Entrance and Kitchen**: Requires 1FA. Users provide either an RFID card, a fingerprint, or a QR code.
- **Warehouse**: Requires 2FA (RFID card and fingerprint). Restricted to Team Leader, IT Dept, and Admin roles.
- **Archive**: Requires 3FA (RFID card, fingerprint, and QR code). Restricted to Team Leader and Admin roles.

## How to Run

1. **Start the Backend**
   Run the database and Spring Boot server using Docker.
   ```bash
   docker-compose up -d --build
   ```

2. **Start the QR Scanner**
   Run the python script on the computer connected to the webcam.
   ```bash
   cd python-qr
   source venv/bin/activate
   pip install -r requirements.txt
   python3 qr_scanner.py
   ```

3. **Deploy the ESP32 Code**
   Open `esp32/mfa_unified/mfa_unified.ino` in the Arduino IDE.
   Update the Wi-Fi credentials and server IP address variables at the top of the file.
   Compile and upload to the ESP32.
