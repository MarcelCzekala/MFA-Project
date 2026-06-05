import cv2
from pyzbar.pyzbar import decode
import requests
import time

SERVER_URL = "http://localhost:8080/api/verify/camera-scan"
CAMERA_INDEX = 0
COOLDOWN_TIME = 3

def scan_qr():
    # init camera stream
    cap = cv2.VideoCapture(CAMERA_INDEX)
    if not cap.isOpened():
        print("Error: Could not open webcam.")
        return

    print("MFA Dumb Scanner started. Waiting for QR codes... (Press 'q' to quit)")
    last_scanned_token = None
    last_scan_time = 0

    while True:
        ret, frame = cap.read()
        if not ret:
            print("Error: Failed to grab frame.")
            break

        detected_codes = decode(frame)

        for qr in detected_codes:
            qr_data = qr.data.decode('utf-8')

            if qr_data.startswith("QR_TOKEN:"):
                token = qr_data.replace("QR_TOKEN:", "")
                current_time = time.time()

                if token != last_scanned_token or (current_time - last_scan_time) > COOLDOWN_TIME:
                    print(f"QR Code detected! Token: {token}")
                    send_token_to_backend(token)

                    last_scanned_token = token
                    last_scan_time = current_time

            points = qr.polygon
            if len(points) == 4:
                pts = [ (p.x, p.y) for p in points ]
                for i in range(4):
                    cv2.line(frame, pts[i], pts[(i+1)%4], (0, 255, 0), 3)

        cv2.imshow("MFA QR Scanner Preview", frame)

        if cv2.waitKey(1) & 0xFF == ord('q'):
            break

    cap.release()
    cv2.destroyAllWindows()

def send_token_to_backend(token):
    # send token to backend
    payload = {"token": token}
    headers = {"X-Hardware-Token": "mfa-hardware-secret-2026"}
    try:
        response = requests.post(SERVER_URL, json=payload, headers=headers, timeout=5)
        if response.status_code == 200:
            print("Backend Response: Token accepted and user cached.")
        else:
            print(f"Backend Error: HTTP {response.status_code} - {response.text}")
    except requests.exceptions.RequestException as e:
        print(f"Network Error: Could not connect to backend. Details: {e}")

if __name__ == "__main__":
    scan_qr()