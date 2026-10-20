/*
 * =============================================================================
 * Guardian Connect — Arduino Heart Rate Sender
 * =============================================================================
 *
 * HARDWARE WIRING
 * ---------------
 * Pulse Sensor (or KY-039 / MAX30102 analog-out / any analog pulse source):
 *   SIGNAL / S  -> A0
 *   VCC         -> 3.3V  (check your sensor! most are 3.3V tolerant, some are 5V)
 *   GND         -> GND
 *
 * HC-05 Bluetooth module:
 *   VCC  -> 5V      (HC-05 onboard regulator accepts 3.6–6V)
 *   GND  -> GND
 *   TXD  -> Arduino D10   (HC-05 TX goes into Arduino RX pin of SoftwareSerial)
 *   RXD  -> Arduino D11 through a voltage divider (2k in series + 1k to GND)
 *           because HC-05 logic is 3.3V and Arduino outputs 5V.
 *           If you skip the divider it usually still works, but is not ideal.
 *
 * POWER IT FROM USB while testing. Only swap to battery AFTER you confirm
 * it works with the Serial Monitor.
 *
 * HOW TO TEST BEFORE THE PHONE
 * ----------------------------
 * 1. Upload this sketch.
 * 2. Open Serial Monitor at 9600 baud. You should see lines like "BPM:74".
 * 3. Pair the HC-05 with your phone from Android Bluetooth settings
 *    (PIN 1234 or 0000). Default name is "HC-05".
 * 4. Install & run the Guardian Connect app, then tap "Connect to HC-05".
 *
 * DATA FORMAT
 * -----------
 * One line per reading, terminated with "\n":
 *    BPM:75
 * The Android side also accepts a bare number: "75\n".
 *
 * NOTE ON THE ALGORITHM
 * ---------------------
 * This is a SIMPLIFIED peak-detection loop, suitable for a college demo with
 * a basic analog pulse sensor. It samples A0 every 2 ms, detects rising
 * crossings past an adaptive threshold, and uses the interval between peaks
 * to compute BPM. For a real medical device you would use a library like
 * PulseSensor Playground, but this keeps the code understandable.
 * =============================================================================
 */

#include <SoftwareSerial.h>

// HC-05: RX of Arduino = pin 10 (reads HC-05 TX),
//        TX of Arduino = pin 11 (writes to HC-05 RX).
SoftwareSerial btSerial(10, 11);

const int PULSE_PIN = A0;

// Timing
unsigned long lastSampleMs = 0;
const unsigned long SAMPLE_INTERVAL_MS = 2;   // 500 Hz sampling

unsigned long lastBeatMs = 0;
unsigned long lastSendMs = 0;
const unsigned long SEND_INTERVAL_MS = 1000;  // send BPM once per second

// Adaptive peak tracking
int signalMin = 1023;
int signalMax = 0;
int threshold = 512;
bool rising = false;

// BPM smoothing
float bpm = 0;
const int BPM_HISTORY = 5;
float bpmHistory[BPM_HISTORY] = {0};
int bpmIndex = 0;

void setup() {
  Serial.begin(9600);     // for debugging on USB Serial Monitor
  btSerial.begin(9600);   // HC-05 default baud. If yours is 38400 change this.
  pinMode(PULSE_PIN, INPUT);

  Serial.println(F("Guardian Connect heart rate sender ready."));
}

void loop() {
  unsigned long now = millis();

  // -------- 1. Sample the pulse signal at a fixed rate --------
  if (now - lastSampleMs >= SAMPLE_INTERVAL_MS) {
    lastSampleMs = now;
    int raw = analogRead(PULSE_PIN);

    // Track rolling min / max so the threshold adapts to the subject.
    if (raw < signalMin) signalMin = raw;
    if (raw > signalMax) signalMax = raw;

    // Recompute threshold at the midpoint of min/max.
    threshold = (signalMin + signalMax) / 2;

    // Gently decay min/max so the threshold keeps tracking.
    // (Avoids it getting stuck if the finger moves.)
    signalMin += 1;
    signalMax -= 1;
    if (signalMin > signalMax) {
      // sanity reset
      signalMin = raw;
      signalMax = raw;
    }

    // -------- 2. Detect rising edge through the threshold --------
    if (!rising && raw > threshold + 10) {
      rising = true;

      unsigned long beatInterval = now - lastBeatMs;
      lastBeatMs = now;

      // Only accept plausible intervals: 300ms (200 BPM) to 2000ms (30 BPM).
      if (beatInterval > 300 && beatInterval < 2000) {
        float instantBpm = 60000.0 / (float) beatInterval;

        // Rolling average over last BPM_HISTORY beats for stability.
        bpmHistory[bpmIndex] = instantBpm;
        bpmIndex = (bpmIndex + 1) % BPM_HISTORY;

        float sum = 0;
        int valid = 0;
        for (int i = 0; i < BPM_HISTORY; i++) {
          if (bpmHistory[i] > 0) { sum += bpmHistory[i]; valid++; }
        }
        if (valid > 0) bpm = sum / valid;
      }
    } else if (rising && raw < threshold - 10) {
      rising = false;
    }
  }

  // -------- 3. Emit BPM over Bluetooth once per second --------
  if (now - lastSendMs >= SEND_INTERVAL_MS) {
    lastSendMs = now;
    int bpmInt = (int) bpm;

    // Clamp to a sensible range for display. If no beat has been seen yet,
    // send 0 so the phone shows we're alive but idle.
    if (bpmInt < 0)   bpmInt = 0;
    if (bpmInt > 220) bpmInt = 220;

    // IMPORTANT: both Serial and btSerial output the SAME line, so you can
    // debug with USB OR Bluetooth without changing the code.
    Serial.print(F("BPM:"));
    Serial.println(bpmInt);

    btSerial.print(F("BPM:"));
    btSerial.println(bpmInt);
  }
}
