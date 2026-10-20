# Guardian Connect — Complete Setup Guide

A beginner-friendly Android app for a college mini project. Connects to an HC-05 Bluetooth module, displays live heart rate from an Arduino, and has an SOS button that sends an SMS.

---

## Table of contents

1. What's in this zip
2. What you need (hardware + software)
3. Part A — Create the Android project in Android Studio
4. Part B — Paste the code files (exact locations)
5. Part C — Wire up the Arduino + HC-05
6. Part D — Pair and run the demo
7. Troubleshooting
8. Customization (change emergency number, HC-05 name, etc.)

---

## 1. What's in this zip

```
GuardianConnect/
├── app/src/main/
│   ├── AndroidManifest.xml                    ← app permissions + activities
│   ├── java/com/example/guardianconnect/
│   │   ├── SplashActivity.java                ← 2-second logo screen
│   │   ├── LoginActivity.java                 ← email/password screen
│   │   └── DashboardActivity.java             ← BPM + SOS (main screen)
│   └── res/
│       ├── layout/
│       │   ├── activity_splash.xml
│       │   ├── activity_login.xml
│       │   └── activity_dashboard.xml
│       ├── values/
│       │   ├── colors.xml
│       │   ├── strings.xml
│       │   └── themes.xml
│       └── drawable/
│           ├── bg_gradient.xml, bg_card.xml, bg_edittext.xml
│           ├── bg_btn_primary.xml, bg_btn_outline.xml, bg_btn_sos.xml
│           ├── dot_connected.xml, dot_disconnected.xml
│           └── ic_heart.xml
├── arduino/
│   ├── heart_rate_sender.ino                  ← real sensor version
│   └── heart_rate_simulator.ino               ← fake-data backup for demo
├── app_build_gradle_reference.txt
└── SETUP_GUIDE.md                             ← this file
```

---

## 2. What you need

**Software**
- Android Studio (any recent version — Hedgehog, Iguana, Jellyfish, or newer)
- Arduino IDE
- A real Android phone (Bluetooth does not work on the Android Studio emulator)

**Hardware**
- Arduino Uno or Nano
- HC-05 Bluetooth module
- Pulse sensor (generic "Pulse Sensor" or KY-039 — any analog pulse sensor works)
- Jumper wires, breadboard
- (Optional) 1 kΩ and 2 kΩ resistors for a voltage divider on HC-05 RX

---

## 3. Part A — Create the Android project

1. Open Android Studio.
2. **File → New → New Project**.
3. Pick **"Empty Views Activity"** (do NOT pick the Compose one).
4. Fill in:
   - **Name:** `Guardian Connect`
   - **Package name:** `com.example.guardianconnect` (must match exactly, or you'll have to edit the `package` line at the top of every `.java` file)
   - **Language:** **Java**
   - **Minimum SDK:** `API 21: Android 5.0 (Lollipop)`
   - **Build configuration language:** Groovy DSL (this is what most tutorials use)
5. Click **Finish** and wait for Gradle sync to complete (status bar at bottom).

Android Studio will have created some default files: `MainActivity.java`, `activity_main.xml`, etc. **You can delete those** — this project replaces them with the three activities below.

---

## 4. Part B — Paste the code files

### Where to paste each file

> **Tip:** Android Studio's **Project view** (top-left dropdown → switch to "Project") shows the real folders exactly as in the zip.

| File from the zip | Paste it into Android Studio at |
|---|---|
| `app/src/main/AndroidManifest.xml` | Replace the existing `app/manifests/AndroidManifest.xml` |
| `SplashActivity.java` | `app/java/com.example.guardianconnect/` |
| `LoginActivity.java` | `app/java/com.example.guardianconnect/` |
| `DashboardActivity.java` | `app/java/com.example.guardianconnect/` |
| `activity_splash.xml` | `app/res/layout/` |
| `activity_login.xml` | `app/res/layout/` |
| `activity_dashboard.xml` | `app/res/layout/` |
| `colors.xml` | Replace the existing `app/res/values/colors.xml` |
| `strings.xml` | Replace the existing `app/res/values/strings.xml` |
| `themes.xml` | Replace the existing `app/res/values/themes.xml` |
| All `drawable/*.xml` files | `app/res/drawable/` |

### Step-by-step

1. **Delete the default MainActivity** that Android Studio generated:
   - Right-click `MainActivity.java` → Delete.
   - Right-click `activity_main.xml` → Delete.

2. **Add the three activities.** Right-click `java/com.example.guardianconnect/` → **New → Java Class** for each one. Name them `SplashActivity`, `LoginActivity`, `DashboardActivity`. Paste the code from the zip, replacing the entire file contents.

3. **Add the three layouts.** Right-click `res/layout/` → **New → Layout Resource File** for each. Name them `activity_splash`, `activity_login`, `activity_dashboard`. Paste the XML, replacing the default `<LinearLayout>`.

4. **Add the drawables.** Right-click `res/drawable/` → **New → Drawable Resource File** for each drawable. Name them exactly the filenames in the zip (without `.xml`). Paste the XML.

5. **Replace values files.** Open each of `colors.xml`, `strings.xml`, `themes.xml` in `res/values/` and replace with the zip version.

6. **Replace the manifest** at `app/manifests/AndroidManifest.xml`.

7. **Sync Gradle** (click the elephant icon with the arrow in the toolbar, or just wait — it should auto-sync).

If everything compiles, you'll see **Build → Make Project → "BUILD SUCCESSFUL"** in the Run panel.

---

## 5. Part C — Wire up the Arduino + HC-05

### Pulse sensor (signal)

| Pulse sensor pin | Arduino pin |
|---|---|
| S / SIGNAL       | A0 |
| VCC              | 3.3V |
| GND              | GND |

### HC-05 Bluetooth module

| HC-05 pin | Arduino pin |
|---|---|
| VCC  | 5V |
| GND  | GND |
| TXD  | D10 |
| RXD  | D11 **(via voltage divider — see note)** |

**About the voltage divider:** the HC-05 RX pin is 3.3V-tolerant but Arduino outputs 5V. The safe way is:

```
Arduino D11 ──[ 2 kΩ ]──┬── HC-05 RXD
                         │
                        [ 1 kΩ ]
                         │
                        GND
```

For a 5-minute college demo you can connect D11 directly to HC-05 RXD and it will usually work, but don't leave it like that long-term.

### Upload the Arduino code

1. Open Arduino IDE.
2. Open `arduino/heart_rate_sender.ino` from the zip.
3. Select your board (Tools → Board → Arduino Uno) and port (Tools → Port).
4. Click **Upload** (right-arrow icon).
5. Open **Tools → Serial Monitor**, set baud to **9600**. You should see `BPM:75` lines once your finger is on the sensor.

> **Backup plan:** if the pulse sensor is noisy or broken on demo day, upload `heart_rate_simulator.ino` instead. It sends fake but realistic BPM numbers so you can still demo the app end-to-end.

---

## 6. Part D — Pair and run the demo

### 1. Pair HC-05 with your phone (do this ONCE from system settings)

1. Power on the Arduino. The HC-05 LED will blink rapidly.
2. On your phone: **Settings → Bluetooth → Pair new device**.
3. Tap **HC-05** when it appears.
4. Enter PIN **`1234`** (or **`0000`** if `1234` doesn't work).
5. You should now see "HC-05" listed under paired devices.

### 2. Install the app

1. In Android Studio: plug your phone in via USB, enable **Developer options** and **USB debugging** on the phone.
2. Click the green **▶ Run** button. Pick your phone from the device list.
3. The splash screen appears for 2 seconds, then the login screen.
4. Enter any email/password and tap **Login**.
5. On the Dashboard, tap **Connect to HC-05**.
6. Grant the Bluetooth permission when prompted.
7. Within ~2 seconds you should see the status dot turn green ("Connected to HC-05") and the BPM number start updating every second.

### 3. Test the SOS

1. Before pressing SOS, open `DashboardActivity.java` and change this line to your own phone number:
   ```java
   private static final String EMERGENCY_NUMBER = "+919876543210";
   ```
   (Use international format with `+` and country code.)
2. Re-run the app.
3. Tap the red **SOS – Send Alert** button → tap **Send** on the confirmation dialog.
4. Grant the SMS permission if asked.
5. A real SMS will go out: *"SOS! I need help. My current heart rate is 75 BPM. …"*

### 4. Test the Emergency Call button

Tap **Call Emergency Contact** — your phone's dialer opens with the number already filled in. Tap the call button to actually place the call. (We intentionally don't auto-dial; it's safer and doesn't need the CALL_PHONE permission.)

---

## 7. Troubleshooting

| Problem | Fix |
|---|---|
| "HC-05 not found in paired devices" | Pair it from **Settings → Bluetooth** first, with PIN `1234` or `0000`. |
| Connects but no data appears | Check Arduino Serial Monitor shows `BPM:xx`. If it does, the Arduino is fine — make sure HC-05 TX is on D10 and RX is on D11. Also confirm HC-05 baud rate is 9600 (some modules ship at 38400; change `btSerial.begin(9600)` in the sketch if yours is different). |
| App crashes on connect | In Android 12+ you MUST grant `BLUETOOTH_CONNECT` permission. Uninstall the app and reinstall to re-trigger the prompt. |
| "Connection failed" immediately | The SPP socket sometimes fails the first time. Toggle Bluetooth off/on on the phone and try again. |
| SOS button → "Failed to send SOS message" | The `SEND_SMS` permission was denied, or the device has no SIM / no signal. Check **Settings → Apps → Guardian Connect → Permissions**. |
| Upload to Arduino fails with "avrdude error" | Disconnect HC-05 **RX/TX from D10/D11 while uploading**. The USB upload shares those pins. Reconnect after upload completes. |
| App opens but crashes immediately | Run **Logcat** in Android Studio and filter for "AndroidRuntime". Usually a typo in a layout ID. |

---

## 8. Customization

All the things you might want to change are at the top of `DashboardActivity.java`:

```java
private static final String EMERGENCY_NUMBER = "+919876543210"; // ← your emergency contact
private static final String HC05_NAME        = "HC-05";          // ← your module name
```

Other changes:

| What to change | Where |
|---|---|
| App name on phone | `res/values/strings.xml` → `app_name` |
| Splash duration | `SplashActivity.java` → `SPLASH_DURATION_MS` |
| Hardcoded login credentials | `LoginActivity.java` → search for the commented-out block |
| Colors / theme | `res/values/colors.xml` |
| SOS message text | `res/values/strings.xml` → `sos_text` |

---

## What each file does (in one line)

- **SplashActivity** — waits 2 s, launches Login
- **LoginActivity** — validates the fields, launches Dashboard
- **DashboardActivity** — owns the Bluetooth socket, the reader thread, the BPM TextView, and the SOS / emergency-call buttons
- **heart_rate_sender.ino** — samples A0, detects peaks, sends `"BPM:xx\n"` over HC-05 every second
- **AndroidManifest.xml** — declares permissions (Bluetooth for both old and new Androids, SMS, Call) and lists the three activities

Good luck with the demo!
