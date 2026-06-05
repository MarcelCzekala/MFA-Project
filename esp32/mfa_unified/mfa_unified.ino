#include <Wire.h>
#include <SPI.h>
#include <MFRC522.h>
#include <LiquidCrystal_I2C.h>
#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include <Adafruit_Fingerprint.h>
#include <HardwareSerial.h>
static const char *API_TOKEN = "mfa-hardware-secret-2026";
static const char *WIFI_SSID = "a";
static const char *WIFI_PASSWORD = "a";
static const char *SERVER_HOST = "a";
static const uint16_t SERVER_PORT = 8080;
static const int PIN_RFID_SS = 5;
static const int PIN_RFID_RST = 14;
static const int PIN_RFID_SCK = 18;
static const int PIN_RFID_MISO = 19;
static const int PIN_RFID_MOSI = 23;
static const int PIN_LCD_SDA = 21;
static const int PIN_LCD_SCL = 22;
static const int PIN_BUZZER = 13;
static const int PIN_LED_GREEN = 4;
static const int PIN_LED_RED = 15;
static const int PIN_JOY_VRX = 34;
static const int PIN_JOY_SW = 32;
static const uint32_t WIFI_RETRY_MS = 10000;
static const uint32_t RESULT_MS = 3500;
static const uint32_t JOY_DEBOUNCE_MS = 280;
static const uint32_t HTTP_TIMEOUT_MS = 5000;
static const int MENU_LEFT = 1000;
static const int MENU_RIGHT = 3000;
static const unsigned long RESET_HOLD_MS = 5000;
MFRC522 rfid(PIN_RFID_SS, PIN_RFID_RST);
HardwareSerial fingerSerial(2);
Adafruit_Fingerprint finger(&fingerSerial);
LiquidCrystal_I2C lcd(0x27, 16, 2);
enum class AppState : uint8_t {
  HOME, VERIFY_ONE_FACTOR, VERIFY_TWO_NFC, VERIFY_TWO_FINGER, VERIFY_HTTP, VERIFY_RESULT,
  WAIT_FOR_QR, WAIT_FOR_1FA_QR, ENROLL_FETCH_ID, ENROLL_NFC, ENROLL_FINGER, ENROLL_HTTP, ENROLL_RESULT,
  CONFIRM_CLEAR_MEM, CLEAR_MEM_PROCESS, CONFIRM_RESET
};
enum class EnrollFingerPhase : uint8_t {
  PLACE_FIRST, CAPTURE_FIRST, WAIT_REMOVE, PLACE_SECOND, CAPTURE_SECOND, STORE
};
static AppState appState = AppState::HOME;
static EnrollFingerPhase enrollFingerPhase = EnrollFingerPhase::PLACE_FIRST;
static unsigned long lastWifiRetryMs = 0;
static unsigned long lastJoySwMs = 0;
static unsigned long resultUntilMs = 0;
static unsigned long enrollDeadlineMs = 0;
static unsigned long removeFingerDeadlineMs = 0;
static unsigned long menuNavigationDelayMs = 0;
static unsigned long swPressedStartMs = 0;
static int failedFingerAttempts = 0;
static int localFingerFailures = 0;
static unsigned long fingerprintStartTime = 0;
static String scannedNfcUid = "";
static String scannedFingerId = "";
static unsigned long qrWaitStartMs = 0;
static unsigned long lastQrPollMs = 0;
static unsigned long lastSecondTime = 0;
static int remainingSeconds = 0;
static String loggedInUser = "";
static int enrollNextId = -1;
static String enrollNfcUid = "";
static int currentMenuIndex = 0;
static int lastDrawnMenuIndex = -1;
static bool rfidNeedsWakeup = true;
static String currentLocation = "";
static unsigned long greenLedUntil = 0;
static unsigned long redLedBlinkUntil = 0;
static bool isAlarmState = false;
static int globalFailCounter = 0;
// trigger alarm state
void triggerAlarmState() {
  isAlarmState = true;
  globalFailCounter = 3;
  httpLogHardwareAlarm();
}
void resetAlarmState() {
  isAlarmState = false;
  globalFailCounter = 0;
  greenLedUntil = 0;
  redLedBlinkUntil = 0;
  noTone(PIN_BUZZER);
  digitalWrite(PIN_LED_GREEN, LOW);
  digitalWrite(PIN_LED_RED, LOW);
}
void triggerSuccess() {
  greenLedUntil = millis() + 5000;
  globalFailCounter = 0;
  tone(PIN_BUZZER, 1000); delay(150);
  tone(PIN_BUZZER, 1500); delay(150);
  noTone(PIN_BUZZER);
}
void triggerFail() {
  if (currentLocation == "Entrance" || currentLocation == "Kitchen") {
    redLedBlinkUntil = millis() + 5000;
    tone(PIN_BUZZER, 300); delay(800);
    noTone(PIN_BUZZER);
    return;
  }
  globalFailCounter++;
  if (globalFailCounter >= 3) {
    triggerAlarmState();
  } else {
    redLedBlinkUntil = millis() + 5000;
    tone(PIN_BUZZER, 300); delay(800);
    noTone(PIN_BUZZER);
  }
}
void playFeedback(String effect) {
  if (isAlarmState) return;
  if (effect == "CLICK") {
    tone(PIN_BUZZER, 2500); delay(30);
    noTone(PIN_BUZZER);
  }
}
void handleLedsAndAlarm() {
  unsigned long now = millis();
  if (isAlarmState) {
    if ((now / 250) % 2 == 0) digitalWrite(PIN_LED_RED, HIGH);
    else digitalWrite(PIN_LED_RED, LOW);
    if ((now / 500) % 2 == 0) tone(PIN_BUZZER, 1000);
    else tone(PIN_BUZZER, 1500);
  } else {
    if (now < greenLedUntil) {
      digitalWrite(PIN_LED_GREEN, HIGH);
    } else {
      digitalWrite(PIN_LED_GREEN, LOW);
    }
    if (now < redLedBlinkUntil) {
      if ((now / 250) % 2 == 0) digitalWrite(PIN_LED_RED, HIGH);
      else digitalWrite(PIN_LED_RED, LOW);
    } else {
      digitalWrite(PIN_LED_RED, LOW);
    }
  }
}
static String truncate16(const String &s) {
  return (s.length() <= 16) ? s : s.substring(0, 16);
}
static String lastLcdLine1 = "";
static String lastLcdLine2 = "";
static void lcdTwo(const String &a, const String &b) {
  String ta = truncate16(a);
  String tb = truncate16(b);
  if (ta == lastLcdLine1 && tb == lastLcdLine2) return;
  lastLcdLine1 = ta;
  lastLcdLine2 = tb;
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print(ta);
  lcd.setCursor(0, 1);
  lcd.print(tb);
}
static void maintainWifi() {
  if (WiFi.status() == WL_CONNECTED) return;
  unsigned long now = millis();
  if (now - lastWifiRetryMs < WIFI_RETRY_MS) return;
  lastWifiRetryMs = now;
  WiFi.disconnect();
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
}
static void setupWifiOnce() {
  WiFi.mode(WIFI_STA);
  WiFi.disconnect(true);
  delay(1000);
  WiFi.setTxPower(WIFI_POWER_19_5dBm);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  unsigned long t0 = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - t0 < 20000UL) delay(1000);
}
static String readCardUid() {
  if (!rfid.PICC_IsNewCardPresent() || !rfid.PICC_ReadCardSerial()) return "";
  String uid = "";
  for (byte i = 0; i < rfid.uid.size; i++) {
    if (rfid.uid.uidByte[i] < 0x10) uid += "0";
    uid += String(rfid.uid.uidByte[i], HEX);
  }
  uid.toUpperCase();
  rfid.PICC_HaltA();
  rfid.PCD_StopCrypto1();
  return uid;
}
static int readFingerMatch() {
  if (finger.getImage() != FINGERPRINT_OK) return -1;
  if (finger.image2Tz() != FINGERPRINT_OK) return -1;
  if (finger.fingerFastSearch() != FINGERPRINT_OK) return -1;
  return finger.fingerID;
}
static bool enrollCaptureToSlot(uint8_t bufferIndex) {
  unsigned long deadline = millis() + 30000UL;
  while (millis() < deadline) {
    uint8_t p = finger.getImage();
    if (p == FINGERPRINT_OK) return finger.image2Tz(bufferIndex) == FINGERPRINT_OK;
    delay(10);
  }
  return false;
}
// check qr poll backend
static int httpCheckQrPoll(const String &location, String &outMsg) {
  if (WiFi.status() != WL_CONNECTED) return 204;
  String url = String("http://") + SERVER_HOST + ":" + String(SERVER_PORT) + "/api/verify/qr-poll/1fa?location=" + location;
  HTTPClient http;
  http.begin(url);
  http.addHeader("X-Hardware-Token", API_TOKEN);
  http.setTimeout(1000);
  int code = http.GET();
  if (code == 204 || code < 0) {
    http.end();
    return 204;
  }
  String resp = http.getString();
  http.end();
  JsonDocument out;
  if (deserializeJson(out, resp)) return 204;
  if (code == 200) {
    if (out.containsKey("employeeName")) {
      outMsg = out["employeeName"].as<String>();
    }
    return 200;
  } else if (code == 400 || code == 403) {
    if (out.containsKey("message")) {
      outMsg = out["message"].as<String>();
    }
    return code;
  }
  return 204;
}
static void httpLogQrTimeout(const String &location) {
  if (WiFi.status() != WL_CONNECTED) return;
  String url = String("http://") + SERVER_HOST + ":" + String(SERVER_PORT) + "/api/verify/timeout";
  JsonDocument doc;
  doc["location"] = location;
  String body;
  serializeJson(doc, body);
  HTTPClient http;
  http.begin(url);
  http.addHeader("Content-Type", "application/json");
  http.addHeader("X-Hardware-Token", API_TOKEN);
  http.setTimeout((int)HTTP_TIMEOUT_MS);
  http.POST(body);
  http.end();
}
static bool httpVerifyMfa(const String &nfcUid, const String &fingerId, const String &location, bool &granted, String &msg) {
  granted = false;
  if (WiFi.status() != WL_CONNECTED) return false;
  String url = String("http://") + SERVER_HOST + ":" + String(SERVER_PORT) + "/api/verify";
  JsonDocument doc;
  doc["nfcUid"] = nfcUid;
  doc["fingerprintId"] = fingerId;
  doc["location"] = location;
  String body;
  serializeJson(doc, body);
  HTTPClient http;
  http.begin(url);
  http.addHeader("Content-Type", "application/json");
  http.addHeader("X-Hardware-Token", API_TOKEN);
  http.setTimeout((int)HTTP_TIMEOUT_MS);
  int code = http.POST(body);
  String resp = http.getString();
  http.end();
  if (code != 200) return false;
  JsonDocument out;
  if (deserializeJson(out, resp)) return false;
  granted = out["accessGranted"] | false;
  if (out.containsKey("employeeName")) {
    msg = out["employeeName"].as<String>();
  } else {
    msg = out["message"].as<String>();
  }
  return true;
}
void httpLogHardwareAlarm() {
  if (WiFi.status() != WL_CONNECTED) return;
  String url = String("http://") + SERVER_HOST + ":" + String(SERVER_PORT) + "/api/verify/alarm";
  JsonDocument doc;
  doc["status"] = "DENIED";
  doc["message"] = "SECURITY ALARM TRIGGERED";
  doc["location"] = currentLocation;
  String body;
  serializeJson(doc, body);
  HTTPClient http;
  http.begin(url);
  http.addHeader("Content-Type", "application/json");
  http.addHeader("X-Hardware-Token", API_TOKEN);
  http.setTimeout((int)HTTP_TIMEOUT_MS);
  http.POST(body);
  http.end();
}
static bool httpGetNextId(int &nextId) {
  nextId = -1;
  if (WiFi.status() != WL_CONNECTED) return false;
  String url = String("http://") + SERVER_HOST + ":" + String(SERVER_PORT) + "/api/enroll/next-available-id";
  HTTPClient http;
  http.begin(url);
  http.addHeader("X-Hardware-Token", API_TOKEN);
  http.setTimeout((int)HTTP_TIMEOUT_MS);
  int code = http.GET();
  String resp = http.getString();
  http.end();
  if (code != 200) return false;
  JsonDocument doc;
  if (deserializeJson(doc, resp)) return false;
  nextId = (int)doc["nextId"].as<long>();
  return true;
}
static bool httpRegisterUser(int nextId, const String &nfcUid, int fingerId) {
  if (WiFi.status() != WL_CONNECTED) return false;
  String url = String("http://") + SERVER_HOST + ":" + String(SERVER_PORT) + "/api/enroll";
  JsonDocument doc;
  doc["nextId"] = nextId;
  doc["nfcUid"] = nfcUid;
  doc["fingerprintId"] = String(fingerId);
  String body;
  serializeJson(doc, body);
  HTTPClient http;
  http.begin(url);
  http.addHeader("Content-Type", "application/json");
  http.addHeader("X-Hardware-Token", API_TOKEN);
  http.setTimeout((int)HTTP_TIMEOUT_MS);
  int code = http.POST(body);
  http.end();
  return (code == 201 || code == 200);
}
static void goHome() {
  appState = AppState::HOME;
  scannedNfcUid = "";
  scannedFingerId = "";
  currentLocation = "";
  currentMenuIndex = 0;
  lastDrawnMenuIndex = -1;
  swPressedStartMs = 0;
  rfidNeedsWakeup = true;
}
static void readJoystickMenu(int maxIndex) {
  int v = analogRead(PIN_JOY_VRX);
  unsigned long now = millis();
  if (now - menuNavigationDelayMs > 250) {
    if (v < MENU_LEFT) {
      if (currentMenuIndex > 0) { currentMenuIndex--; menuNavigationDelayMs = now; playFeedback("CLICK"); }
    } else if (v > MENU_RIGHT) {
      if (currentMenuIndex < maxIndex) { currentMenuIndex++; menuNavigationDelayMs = now; playFeedback("CLICK"); }
    }
  }
}
static bool joyPressed() {
  static bool lastBtnState = HIGH;
  bool currentBtnState = digitalRead(PIN_JOY_SW);
  if (currentBtnState == LOW && lastBtnState == HIGH) {
    unsigned long now = millis();
    if (now - lastJoySwMs > 50) {
      lastJoySwMs = now;
      lastBtnState = LOW;
      playFeedback("CLICK");
      return true;
    }
  }
  if (currentBtnState == HIGH) {
    lastBtnState = HIGH;
  }
  return false;
}
static void drawHomeMenu() {
  String menus[] = {"Entrance", "Kitchen", "Warehouse", "Archive", "Enroll", "Clear Memory"};
  String line1 = (currentMenuIndex % 2 == 0) ? ("> " + menus[currentMenuIndex]) : ("  " + menus[currentMenuIndex - 1]);
  String line2 = (currentMenuIndex % 2 == 1) ? ("> " + menus[currentMenuIndex]) : ((currentMenuIndex + 1 < 6) ? ("  " + menus[currentMenuIndex + 1]) : "");
  lcdTwo(line1, line2);
}
static void tickHome() {
  readJoystickMenu(5);
  if (currentMenuIndex != lastDrawnMenuIndex) { lastDrawnMenuIndex = currentMenuIndex; drawHomeMenu(); }
  if (!joyPressed()) return;
  if (currentMenuIndex <= 3) {
    String menus[] = {"Entrance", "Kitchen", "Warehouse", "Archive"};
    currentLocation = menus[currentMenuIndex];
    scannedNfcUid = "";
    scannedFingerId = "";
    if (currentLocation == "Entrance" || currentLocation == "Kitchen") {
      appState = AppState::VERIFY_ONE_FACTOR;
      lcdTwo("Card/Finger OR", "Click for QR");
    } else {
      appState = AppState::VERIFY_TWO_NFC;
      lcdTwo("Scan Card", "...");
    }
  }
  else if (currentMenuIndex == 4) { appState = AppState::ENROLL_FETCH_ID; lcdTwo("Fetching ID...", "Please wait"); }
  else if (currentMenuIndex == 5) { appState = AppState::CONFIRM_CLEAR_MEM; lcdTwo("Clear memory?", "Click to confirm"); }
}
// handle one factor verify
static void tickVerifyOneFactor() {
  if (joyPressed()) {
    qrWaitStartMs = millis();
    lastQrPollMs = 0;
    lastSecondTime = millis();
    remainingSeconds = 60;
    appState = AppState::WAIT_FOR_1FA_QR;
    lcdTwo("Scan QR on PC", "01:00");
    return;
  }
  if (rfidNeedsWakeup) { rfid.PCD_Init(); rfidNeedsWakeup = false; }
  String uid = readCardUid();
  if (uid.length() > 0) {
    scannedNfcUid = uid;
    appState = AppState::VERIFY_HTTP;
    lcdTwo("Card read.", "Verifying...");
    playFeedback("CLICK");
    return;
  }
  int id = readFingerMatch();
  if (id > 0) {
    scannedFingerId = String(id);
    appState = AppState::VERIFY_HTTP;
    lcdTwo("Finger read.", "Verifying...");
    return;
  } else {
    if (finger.getImage() == FINGERPRINT_OK) {
      localFingerFailures++;
      if (localFingerFailures >= 3) {
        localFingerFailures = 0;
        triggerFail();
        if (globalFailCounter >= 3) {
          lcdTwo("ALARM!", "System locked");
        } else {
          lcdTwo("Access Denied", "Mismatch/Failed");
        }
        appState = AppState::VERIFY_RESULT; resultUntilMs = millis() + RESULT_MS;
      } else {
        lcdTwo("Wrong Finger", "Try again...");
        delay(1500);
        lcdTwo("Card/Finger OR", "Click for QR");
      }
      while (finger.getImage() != FINGERPRINT_NOFINGER) { delay(100); }
    }
  }
}
static void tickVerifyTwoNfc() {
  if (rfidNeedsWakeup) { rfid.PCD_Init(); rfidNeedsWakeup = false; }
  String uid = readCardUid();
  if (uid.length() > 0) {
    scannedNfcUid = uid;
    appState = AppState::VERIFY_TWO_FINGER;
    lcdTwo("Card OK", "Scan finger");
    playFeedback("CLICK");
    fingerprintStartTime = 0;
    localFingerFailures = 0;
  }
}
static void tickVerifyTwoFinger() {
  if (fingerprintStartTime == 0) { fingerprintStartTime = millis(); localFingerFailures = 0; }
  int id = readFingerMatch();
  if (id > 0) {
    scannedFingerId = String(id);
    appState = AppState::VERIFY_HTTP;
    lcdTwo("Sending...", "Please wait");
  } else {
    unsigned long now = millis();
    if (finger.getImage() == FINGERPRINT_OK) {
      localFingerFailures++;
      if (localFingerFailures >= 3) {
        localFingerFailures = 0;
        triggerFail();
        if (globalFailCounter >= 3) {
          lcdTwo("ALARM!", "System locked");
        } else {
          bool granted = false; String msg = "";
          httpVerifyMfa(scannedNfcUid, "", currentLocation, granted, msg);
          String msg2 = (currentLocation == "Warehouse" || currentLocation == "Archive") ? ("Tries left: " + String(3 - globalFailCounter)) : "Mismatch/Failed";
          lcdTwo("Access Denied", msg2);
        }
        appState = AppState::VERIFY_RESULT; resultUntilMs = millis() + RESULT_MS;
      } else {
        lcdTwo("Wrong Finger", "Try again...");
        delay(1500);
        lcdTwo("Scan finger", "...");
      }
      while (finger.getImage() != FINGERPRINT_NOFINGER) { delay(100); }
    }
    if (now - fingerprintStartTime > 15000UL) {
      lcdTwo("Auth Failed", "Timeout"); triggerFail();
      appState = AppState::VERIFY_RESULT; resultUntilMs = millis() + RESULT_MS;
    }
  }
}
static void tickVerifyHttp() {
  bool granted = false; String msg = "";
  if (httpVerifyMfa(scannedNfcUid, scannedFingerId, currentLocation, granted, msg)) {
    if (granted) {
      if (currentLocation == "Archive") {
        loggedInUser = msg;
        qrWaitStartMs = millis();
        lastQrPollMs = 0;
        lastSecondTime = millis();
        remainingSeconds = 300;
        appState = AppState::WAIT_FOR_QR;
        lcdTwo("Scan QR on PC", "05:00");
        return;
      } else {
        lcdTwo("Welcome,", truncate16(msg));
        triggerSuccess();
      }
    }
    else {
      triggerFail();
      if (globalFailCounter >= 3) {
        lcdTwo("ALARM!", "System locked");
      } else {
        String msg2 = (currentLocation == "Warehouse" || currentLocation == "Archive") ? ("Tries left: " + String(3 - globalFailCounter)) : "Access Denied";
        lcdTwo("Failed", msg2);
      }
    }
  } else {
    triggerFail();
    if (globalFailCounter >= 3) {
      lcdTwo("ALARM!", "System locked");
    } else {
      String msg2 = (currentLocation == "Warehouse" || currentLocation == "Archive") ? ("Tries left: " + String(3 - globalFailCounter)) : "Access Denied";
      lcdTwo("Net Error", msg2);
    }
  }
  appState = AppState::VERIFY_RESULT; resultUntilMs = millis() + RESULT_MS;
}
static void tickVerifyResult() { if (millis() >= resultUntilMs) goHome(); }
// handle qr wait state
static void tickWaitForQr() {
  unsigned long currentMillis = millis();
  if (remainingSeconds <= 0) {
    httpLogQrTimeout(currentLocation);
    triggerFail();
    lcdTwo("Auth Failed", "Timeout");
    appState = AppState::VERIFY_RESULT; resultUntilMs = currentMillis + RESULT_MS;
    return;
  }
  if (joyPressed() || analogRead(PIN_JOY_VRX) < MENU_LEFT) {
    goHome();
    return;
  }
  if (currentMillis - lastSecondTime >= 1000) {
    lastSecondTime += 1000;
    remainingSeconds--;
    char buf[16];
    snprintf(buf, sizeof(buf), "%02d:%02d", remainingSeconds / 60, remainingSeconds % 60);
    lcdTwo("Scan QR on PC", buf);
  }
  if (currentMillis - lastQrPollMs >= 2000UL) {
    lastQrPollMs = currentMillis;
    String outMsg = "";
    int pollCode = httpCheckQrPoll(currentLocation, outMsg);
    if (pollCode == 200) {
      lcdTwo("Welcome,", truncate16(outMsg));
      triggerSuccess();
      appState = AppState::VERIFY_RESULT; resultUntilMs = currentMillis + RESULT_MS;
    } else if (pollCode == 400 || pollCode == 403) {
      lcdTwo("Access Denied", truncate16(outMsg));
      triggerFail();
      appState = AppState::VERIFY_RESULT; resultUntilMs = currentMillis + RESULT_MS;
    }
  }
}
static void tickWaitFor1faQr() {
  unsigned long currentMillis = millis();
  if (remainingSeconds <= 0) {
    lcdTwo("Auth Failed", "Timeout");
    redLedBlinkUntil = currentMillis + 3000;
    tone(PIN_BUZZER, 300); delay(800); noTone(PIN_BUZZER);
    appState = AppState::VERIFY_RESULT; resultUntilMs = currentMillis + RESULT_MS;
    return;
  }
  if (joyPressed() || analogRead(PIN_JOY_VRX) < MENU_LEFT) {
    goHome();
    return;
  }
  if (currentMillis - lastSecondTime >= 1000) {
    lastSecondTime += 1000;
    remainingSeconds--;
    char buf[16];
    snprintf(buf, sizeof(buf), "%02d:%02d", remainingSeconds / 60, remainingSeconds % 60);
    lcdTwo("Scan QR on PC", buf);
  }
  if (currentMillis - lastQrPollMs >= 2000UL) {
    lastQrPollMs = currentMillis;
    String outMsg = "";
    int pollCode = httpCheckQrPoll(currentLocation, outMsg);
    if (pollCode == 200) {
      lcdTwo("Welcome,", truncate16(outMsg));
      triggerSuccess();
      appState = AppState::VERIFY_RESULT; resultUntilMs = currentMillis + RESULT_MS;
    } else if (pollCode == 400 || pollCode == 403) {
      lcdTwo("Access Denied", truncate16(outMsg));
      triggerFail();
      appState = AppState::VERIFY_RESULT; resultUntilMs = currentMillis + RESULT_MS;
    }
  }
}
static void tickEnrollFetchId() {
  int id = -1;
  if (httpGetNextId(id)) { enrollNextId = id; appState = AppState::ENROLL_NFC; lcdTwo("Auto ID:", String(id)); delay(800); lcdTwo("Scan NFC card", "..."); }
  else { lcdTwo("Server Error", "Check Spring"); triggerFail(); appState = AppState::ENROLL_RESULT; resultUntilMs = millis() + RESULT_MS; }
}
static void tickEnrollNfc() {
  if (rfidNeedsWakeup) { rfid.PCD_Init(); rfidNeedsWakeup = false; }
  String uid = readCardUid();
  if (uid.length() == 0) return;
  enrollNfcUid = uid; enrollFingerPhase = EnrollFingerPhase::PLACE_FIRST; enrollDeadlineMs = millis() + 90000UL; appState = AppState::ENROLL_FINGER; lcdTwo("Place finger", "(1st)");
}
static void tickEnrollFinger() {
  if (millis() > enrollDeadlineMs) { lcdTwo("Enroll timeout", ""); triggerFail(); appState = AppState::ENROLL_RESULT; resultUntilMs = millis() + RESULT_MS; return; }
  switch (enrollFingerPhase) {
    case EnrollFingerPhase::PLACE_FIRST: enrollFingerPhase = EnrollFingerPhase::CAPTURE_FIRST; break;
    case EnrollFingerPhase::CAPTURE_FIRST: if (enrollCaptureToSlot(1)) { lcdTwo("Remove finger", "..."); removeFingerDeadlineMs = millis() + 20000UL; enrollFingerPhase = EnrollFingerPhase::WAIT_REMOVE; } break;
    case EnrollFingerPhase::WAIT_REMOVE: if (finger.getImage() == FINGERPRINT_NOFINGER) { lcdTwo("Place again", "(2nd)"); enrollFingerPhase = EnrollFingerPhase::PLACE_SECOND; } break;
    case EnrollFingerPhase::PLACE_SECOND: enrollFingerPhase = EnrollFingerPhase::CAPTURE_SECOND; break;
    case EnrollFingerPhase::CAPTURE_SECOND: if (enrollCaptureToSlot(2)) enrollFingerPhase = EnrollFingerPhase::STORE; break;
    case EnrollFingerPhase::STORE: if (finger.createModel() == FINGERPRINT_OK && finger.storeModel(enrollNextId) == FINGERPRINT_OK) { appState = AppState::ENROLL_HTTP; lcdTwo("Saving user...", "Please wait"); } else { lcdTwo("Store failed", ""); triggerFail(); appState = AppState::ENROLL_RESULT; resultUntilMs = millis() + RESULT_MS; } break;
  }
}
static void tickEnrollHttp() {
  if (httpRegisterUser(enrollNextId, enrollNfcUid, enrollNextId)) { lcdTwo("Enroll OK", "User saved"); triggerSuccess(); }
  else { lcdTwo("Save failed", "Try again"); triggerFail(); }
  appState = AppState::ENROLL_RESULT; resultUntilMs = millis() + RESULT_MS;
}
static void tickEnrollResult() { if (millis() >= resultUntilMs) goHome(); }
static void tickConfirmClearMem() { if (joyPressed()) { appState = AppState::CLEAR_MEM_PROCESS; lcdTwo("Wiping memory", "Please wait..."); } }
static void tickClearMemProcess() {
  if (finger.emptyDatabase() == FINGERPRINT_OK) { lcdTwo("Database Empty", "Wipe Complete"); triggerSuccess(); }
  else { lcdTwo("Wipe Failed", "Driver Error"); triggerFail(); }
  delay(2000); goHome();
}
static void tickConfirmReset() { if (joyPressed()) { playFeedback("SUCCESS"); goHome(); delay(500); } }
void checkGlobalReset() {
  bool currentSwState = digitalRead(PIN_JOY_SW);
  if (currentSwState == LOW) {
    if (swPressedStartMs == 0) swPressedStartMs = millis();
    else if (millis() - swPressedStartMs >= RESET_HOLD_MS) {
      if (isAlarmState) {
        resetAlarmState();
        playFeedback("CLICK");
        swPressedStartMs = 0; delay(500);
      }
      else if (appState != AppState::HOME && appState != AppState::CONFIRM_RESET) {
        playFeedback("CLICK"); appState = AppState::CONFIRM_RESET;
        lcdTwo("Return to menu?", "Click to confirm"); swPressedStartMs = 0; delay(500);
      }
    }
  } else { if (swPressedStartMs > 0) swPressedStartMs = 0; }
}
void setup() {
  Serial.begin(115200);
  pinMode(PIN_JOY_SW, INPUT_PULLUP);
  pinMode(PIN_BUZZER, OUTPUT);
  pinMode(PIN_LED_GREEN, OUTPUT);
  pinMode(PIN_LED_RED, OUTPUT);
  digitalWrite(PIN_LED_GREEN, LOW);
  digitalWrite(PIN_LED_RED, LOW);
  Wire.begin(PIN_LCD_SDA, PIN_LCD_SCL);
  lcd.init(); lcd.backlight();
  SPI.begin(PIN_RFID_SCK, PIN_RFID_MISO, PIN_RFID_MOSI, PIN_RFID_SS);
  SPI.setFrequency(1000000);
  rfid.PCD_Init();
  fingerSerial.begin(57600, SERIAL_8N1, 26, 27);
  if (finger.verifyPassword()) finger.setSecurityLevel(2);
  setupWifiOnce();
  goHome();
}
void loop() {
  handleLedsAndAlarm();
  checkGlobalReset();
  if (isAlarmState) {
    lcdTwo("ALARM!", "System locked");
    delay(10);
    return;
  }
  maintainWifi();
  switch (appState) {
    case AppState::HOME: tickHome(); break;
    case AppState::VERIFY_ONE_FACTOR: tickVerifyOneFactor(); break;
    case AppState::VERIFY_TWO_NFC: tickVerifyTwoNfc(); break;
    case AppState::VERIFY_TWO_FINGER: tickVerifyTwoFinger(); break;
    case AppState::VERIFY_HTTP: tickVerifyHttp(); break;
    case AppState::VERIFY_RESULT: tickVerifyResult(); break;
    case AppState::WAIT_FOR_QR: tickWaitForQr(); break;
    case AppState::WAIT_FOR_1FA_QR: tickWaitFor1faQr(); break;
    case AppState::ENROLL_FETCH_ID: tickEnrollFetchId(); break;
    case AppState::ENROLL_NFC: tickEnrollNfc(); break;
    case AppState::ENROLL_FINGER: tickEnrollFinger(); break;
    case AppState::ENROLL_HTTP: tickEnrollHttp(); break;
    case AppState::ENROLL_RESULT: tickEnrollResult(); break;
    case AppState::CONFIRM_CLEAR_MEM: tickConfirmClearMem(); break;
    case AppState::CLEAR_MEM_PROCESS: tickClearMemProcess(); break;
    case AppState::CONFIRM_RESET: tickConfirmReset(); break;
  }
  delay(1);
}
