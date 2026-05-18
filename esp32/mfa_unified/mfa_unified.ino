#include <Wire.h>
#include <SPI.h>
#include <MFRC522.h>
#include <LiquidCrystal_I2C.h>
#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include <Adafruit_Fingerprint.h>
#include <HardwareSerial.h>

static const char *WIFI_SSID = "hostname";
static const char *WIFI_PASSWORD = "password";
static const char *SERVER_HOST = "host ip";
static const uint16_t SERVER_PORT = 8080;

static const int PIN_RFID_SS = 5;
static const int PIN_RFID_RST = 14;
static const int PIN_RFID_SCK = 18;
static const int PIN_RFID_MISO = 19;
static const int PIN_RFID_MOSI = 23;

static const int PIN_LCD_SDA = 21;
static const int PIN_LCD_SCL = 22;
static const int PIN_BUZZER = 13; 

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
  HOME,
  VERIFY_NFC,
  VERIFY_FINGER,
  VERIFY_HTTP,
  VERIFY_RESULT,
  ENROLL_FETCH_ID,
  ENROLL_NFC,
  ENROLL_FINGER,
  ENROLL_HTTP,
  ENROLL_RESULT,
  CONFIRM_CLEAR_MEM,
  CLEAR_MEM_PROCESS
};

enum class EnrollFingerPhase : uint8_t {
  PLACE_FIRST,
  CAPTURE_FIRST,
  WAIT_REMOVE,
  PLACE_SECOND,
  CAPTURE_SECOND,
  STORE
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

static String scannedNfcUid = "";
static int scannedFingerId = -1;
static int enrollNextId = -1;
static String enrollNfcUid = "";

static int currentMenuIndex = 0; 
static int lastDrawnMenuIndex = -1;
static bool rfidNeedsWakeup = true;

void playFeedback(String effect) {
  if (effect == "SUCCESS") {
    tone(PIN_BUZZER, 1000); delay(150);
    tone(PIN_BUZZER, 1500); delay(150);
    noTone(PIN_BUZZER);
  } 
  else if (effect == "ERROR") {
    tone(PIN_BUZZER, 300); delay(800);
    noTone(PIN_BUZZER);
  } 
  else if (effect == "CLICK") {
    tone(PIN_BUZZER, 2500); delay(30);
    noTone(PIN_BUZZER);
  }
}

static String truncate16(const String &s) {
  return (s.length() <= 16) ? s : s.substring(0, 16);
}

static void lcdTwo(const String &a, const String &b) {
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print(truncate16(a));
  lcd.setCursor(0, 1);
  lcd.print(truncate16(b));
}

static void logHttpError(const char *step, int code, const String &detail) {
  Serial.print("[HTTP] ");
  Serial.print(step);
  Serial.print(" code=");
  Serial.print(code);
  Serial.println();
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
  Serial.print("\n[Wi-Fi] Connecting to: ");
  Serial.println(WIFI_SSID);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  
  unsigned long t0 = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - t0 < 20000UL) {
    delay(1000);
    Serial.print("[Wi-Fi] Current Status Code: ");
    Serial.println(WiFi.status());
  }
  
  if (WiFi.status() == WL_CONNECTED) {
    Serial.print("[Wi-Fi] SUCCESS! Connected. IP: ");
    Serial.println(WiFi.localIP());
  } else {
    Serial.println("[Wi-Fi] CRITICAL: Connection timeout after 20 seconds!");
  }
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
    if (p == FINGERPRINT_OK) {
      return finger.image2Tz(bufferIndex) == FINGERPRINT_OK;
    }
    delay(10);
  }
  return false;
}

static bool httpVerifyMfa(const String &nfcUid, int fingerId, bool &granted, String &msg) {
  granted = false;
  msg = "Network error";
  if (WiFi.status() != WL_CONNECTED) return false;

  String url = String("http://") + SERVER_HOST + ":" + String(SERVER_PORT) + "/api/verify";
  JsonDocument doc;
  doc["nfcUid"] = nfcUid;
  doc["fingerprintId"] = String(fingerId);
  String body;
  serializeJson(doc, body);

  HTTPClient http;
  http.begin(url);
  http.addHeader("Content-Type", "application/json");
  http.setTimeout((int)HTTP_TIMEOUT_MS);
  int code = http.POST(body);
  String resp = http.getString();
  http.end();

  if (code != 200) return false;

  JsonDocument out;
  if (deserializeJson(out, resp)) return false;
  granted = out["accessGranted"] | false;
  msg = out["message"].as<String>();
  return true;
}

static bool httpGetNextId(int &nextId, String &err) {
  nextId = -1;
  if (WiFi.status() != WL_CONNECTED) return false;

  String url = String("http://") + SERVER_HOST + ":" + String(SERVER_PORT) + "/api/enroll/next-available-id";
  HTTPClient http;
  http.begin(url);
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

static bool httpRegisterUser(int nextId, const String &nfcUid, int fingerId, String &err) {
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
  http.setTimeout((int)HTTP_TIMEOUT_MS);
  int code = http.POST(body);
  http.end();

  return (code == 201 || code == 200);
}

static void goHome() {
  appState = AppState::HOME;
  scannedNfcUid = "";
  scannedFingerId = -1;
  currentMenuIndex = 0;
  lastDrawnMenuIndex = -1;
  swPressedStartMs = 0;
  rfidNeedsWakeup = true; 
}

static void readJoystickMenu() {
  int v = analogRead(PIN_JOY_VRX);
  unsigned long now = millis();
  
  if (now - menuNavigationDelayMs > 250) {
    if (v < MENU_LEFT) {
      if (currentMenuIndex > 0) {
        currentMenuIndex--;
        menuNavigationDelayMs = now;
        playFeedback("CLICK");
      }
    } else if (v > MENU_RIGHT) {
      if (currentMenuIndex < 2) {
        currentMenuIndex++;
        menuNavigationDelayMs = now;
        playFeedback("CLICK");
      }
    }
  }
}

static bool joyPressed() {
  if (digitalRead(PIN_JOY_SW) != LOW) return false;
  unsigned long now = millis();
  if (now - lastJoySwMs < JOY_DEBOUNCE_MS) return false;
  lastJoySwMs = now;
  playFeedback("CLICK");
  return true;
}

static void drawHomeMenu() {
  if (currentMenuIndex == 0) lcdTwo("> 1.VERIFY", "  2.ENROLL");
  else if (currentMenuIndex == 1) lcdTwo("  1.VERIFY", "> 2.ENROLL");
  else if (currentMenuIndex == 2) lcdTwo("  2.ENROLL", "> 3.CLEAR MEMORY");
}

static void tickHome() {
  readJoystickMenu();
  if (currentMenuIndex != lastDrawnMenuIndex) {
    lastDrawnMenuIndex = currentMenuIndex;
    drawHomeMenu();
  }
  if (!joyPressed()) return;
  
  if (currentMenuIndex == 0) {
    appState = AppState::VERIFY_NFC;
    lcdTwo("Scan NFC card", "...");
  } else if (currentMenuIndex == 1) {
    appState = AppState::ENROLL_FETCH_ID;
    lcdTwo("Fetching ID...", "Please wait");
  } else if (currentMenuIndex == 2) {
    appState = AppState::CONFIRM_CLEAR_MEM;
    lcdTwo("Clear memory?", "Click to confirm");
  }
}

static bool httpCheckCard(String uid) {
  if (WiFi.status() != WL_CONNECTED) return false;

  String url = String("http://") + SERVER_HOST + ":" + String(SERVER_PORT) + "/api/verify/card/" + uid;
  HTTPClient http;
  http.begin(url);
  http.setTimeout(4000);
  int code = http.GET();
  String resp = http.getString();
  http.end();

  if (code == 200) {
    JsonDocument doc;
    deserializeJson(doc, resp);
    return doc["exists"] | false;
  }
  return false;
}

static void tickVerifyNfc() {
  if (rfidNeedsWakeup) {
    rfid.PCD_Init();
    rfidNeedsWakeup = false;
  }

  String uid = readCardUid();
  if (uid.length() == 0) return;
  lcdTwo("Checking card", "...");
  
  if (httpCheckCard(uid)) {
    scannedNfcUid = uid;
    appState = AppState::VERIFY_FINGER;
    lcdTwo("Card OK", "Scan finger");
    playFeedback("CLICK");
  } else {
    lcdTwo("Unknown Card", "Access Denied");
    playFeedback("ERROR");
    appState = AppState::VERIFY_RESULT;
    resultUntilMs = millis() + 2500;
  }
}

static void tickVerifyFinger() {
  int id = readFingerMatch();
  if (id < 0) return;
  scannedFingerId = id;
  appState = AppState::VERIFY_HTTP;
  lcdTwo("Sending...", "Please wait");
}

static void tickVerifyHttp() {
  bool ok = false, granted = false;
  String msg = "";
  ok = httpVerifyMfa(scannedNfcUid, scannedFingerId, granted, msg);

  if (ok && granted) {
    lcdTwo("Success", "Welcome!");
    playFeedback("SUCCESS");
  } else {
    lcdTwo("Failed", "Access Denied");
    playFeedback("ERROR");
  }
  appState = AppState::VERIFY_RESULT;
  resultUntilMs = millis() + RESULT_MS;
}

static void tickVerifyResult() {
  if (millis() >= resultUntilMs) goHome();
}

static void tickEnrollFetchId() {
  String err;
  int id = -1;

  if (WiFi.status() != WL_CONNECTED) {
    lcdTwo("No Wi-Fi Link", "Reconnecting...");
    playFeedback("ERROR");
    Serial.println("[DEBUG] Fetch ID failed because Wi-Fi status is NOT connected.");
    appState = AppState::ENROLL_RESULT;
    resultUntilMs = millis() + RESULT_MS;
    return;
  }

  if (!httpGetNextId(id, err)) {
    lcdTwo("Server Timeout", "Check Spring");
    playFeedback("ERROR");
    appState = AppState::ENROLL_RESULT;
    resultUntilMs = millis() + RESULT_MS;
    return;
  }
  
  enrollNextId = id;
  appState = AppState::ENROLL_NFC;
  lcdTwo("Auto ID:", String(id));
  delay(800);
  lcdTwo("Scan NFC card", "...");
}

static void tickEnrollNfc() {
  if (rfidNeedsWakeup) {
    rfid.PCD_Init();
    rfidNeedsWakeup = false;
  }

  String uid = readCardUid();
  if (uid.length() == 0) return;
  enrollNfcUid = uid;
  enrollFingerPhase = EnrollFingerPhase::PLACE_FIRST;
  enrollDeadlineMs = millis() + 90000UL;
  appState = AppState::ENROLL_FINGER;
  lcdTwo("Place finger", "(1st)");
}

static void tickEnrollFinger() {
  if (millis() > enrollDeadlineMs) {
    lcdTwo("Enroll timeout", "");
    playFeedback("ERROR");
    appState = AppState::ENROLL_RESULT;
    resultUntilMs = millis() + RESULT_MS;
    return;
  }

  switch (enrollFingerPhase) {
    case EnrollFingerPhase::PLACE_FIRST:
      enrollFingerPhase = EnrollFingerPhase::CAPTURE_FIRST;
      break;
    case EnrollFingerPhase::CAPTURE_FIRST:
      if (enrollCaptureToSlot(1)) {
        lcdTwo("Remove finger", "...");
        removeFingerDeadlineMs = millis() + 20000UL;
        enrollFingerPhase = EnrollFingerPhase::WAIT_REMOVE;
      }
      break;
    case EnrollFingerPhase::WAIT_REMOVE:
      if (finger.getImage() == FINGERPRINT_NOFINGER) {
        lcdTwo("Place again", "(2nd)");
        enrollFingerPhase = EnrollFingerPhase::PLACE_SECOND;
      }
      break;
    case EnrollFingerPhase::PLACE_SECOND:
      enrollFingerPhase = EnrollFingerPhase::CAPTURE_SECOND;
      break;
    case EnrollFingerPhase::CAPTURE_SECOND:
      if (enrollCaptureToSlot(2)) enrollFingerPhase = EnrollFingerPhase::STORE;
      break;
    case EnrollFingerPhase::STORE:
      if (finger.createModel() == FINGERPRINT_OK && finger.storeModel(enrollNextId) == FINGERPRINT_OK) {
        appState = AppState::ENROLL_HTTP;
        lcdTwo("Saving user...", "Please wait");
      } else {
        lcdTwo("Store failed", "");
        playFeedback("ERROR");
        appState = AppState::ENROLL_RESULT;
        resultUntilMs = millis() + RESULT_MS;
      }
      break;
  }
}

static void tickEnrollHttp() {
  String err;
  if (httpRegisterUser(enrollNextId, enrollNfcUid, enrollNextId, err)) {
    lcdTwo("Enroll OK", "User saved");
    playFeedback("SUCCESS");
  } else {
    lcdTwo("Save failed", "Try again");
    playFeedback("ERROR");
  }
  appState = AppState::ENROLL_RESULT;
  resultUntilMs = millis() + RESULT_MS;
}

static void tickEnrollResult() {
  if (millis() >= resultUntilMs) goHome();
}

static void tickConfirmClearMem() {
  if (joyPressed()) {
    appState = AppState::CLEAR_MEM_PROCESS;
    lcdTwo("Wiping memory", "Please wait...");
  }
}

static void tickClearMemProcess() {
  if (finger.emptyDatabase() == FINGERPRINT_OK) {
    lcdTwo("Database Empty", "Wipe Complete");
    playFeedback("SUCCESS");
  } else {
    lcdTwo("Wipe Failed", "Driver Error");
    playFeedback("ERROR");
  }
  delay(2000);
  goHome();
}

void checkGlobalReset() {
  bool currentSwState = digitalRead(PIN_JOY_SW);
  if (currentSwState == LOW) {
    if (swPressedStartMs == 0) swPressedStartMs = millis();
    else if (millis() - swPressedStartMs >= RESET_HOLD_MS) {
      if (appState != AppState::HOME) {
        playFeedback("ERROR");
        goHome();
        delay(600);
      }
    }
  } else {
    swPressedStartMs = 0;
  }
}

void setup() {
  Serial.begin(115200);
  pinMode(PIN_JOY_SW, INPUT_PULLUP);
  pinMode(PIN_BUZZER, OUTPUT);

  Wire.begin(PIN_LCD_SDA, PIN_LCD_SCL);
  lcd.init();
  lcd.backlight();

  SPI.begin(PIN_RFID_SCK, PIN_RFID_MISO, PIN_RFID_MOSI, PIN_RFID_SS);
  SPI.setFrequency(1000000); 
  rfid.PCD_Init();
  rfid.PCD_DumpVersionToSerial(); 

  fingerSerial.begin(57600, SERIAL_8N1, 26, 27);
  if (finger.verifyPassword()) {
    finger.setSecurityLevel(2); 
  }

  setupWifiOnce();
  goHome();
}

void loop() {
  checkGlobalReset();
  maintainWifi();
  
  switch (appState) {
    case AppState::HOME: tickHome(); break;
    case AppState::VERIFY_NFC: tickVerifyNfc(); break;
    case AppState::VERIFY_FINGER: tickVerifyFinger(); break;
    case AppState::VERIFY_HTTP: tickVerifyHttp(); break;
    case AppState::VERIFY_RESULT: tickVerifyResult(); break;
    case AppState::ENROLL_FETCH_ID: tickEnrollFetchId(); break;
    case AppState::ENROLL_NFC: tickEnrollNfc(); break;
    case AppState::ENROLL_FINGER: tickEnrollFinger(); break;
    case AppState::ENROLL_HTTP: tickEnrollHttp(); break;
    case AppState::ENROLL_RESULT: tickEnrollResult(); break;
    case AppState::CONFIRM_CLEAR_MEM: tickConfirmClearMem(); break;
    case AppState::CLEAR_MEM_PROCESS: tickClearMemProcess(); break;
  }
  delay(1);
}