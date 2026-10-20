/*
 * =============================================================================
 * Guardian Connect — Arduino BPM SIMULATOR (no sensor needed)
 * =============================================================================
 *
 * Use this sketch if your pulse sensor is acting up on demo day and you just
 * want to prove the Android app + Bluetooth pipeline works. It sends a
 * realistic-looking BPM number (drifts between 68 and 92) every second.
 *
 * WIRING
 * ------
 * Only the HC-05 is required:
 *   HC-05 VCC  -> 5V
 *   HC-05 GND  -> GND
 *   HC-05 TXD  -> Arduino D10
 *   HC-05 RXD  -> Arduino D11 (via voltage divider, or direct for quick demo)
 *
 * Upload this, pair HC-05 from the phone, tap Connect in the app.
 * =============================================================================
 */

#include <SoftwareSerial.h>

SoftwareSerial btSerial(10, 11); // RX, TX

int bpm = 75;
int direction = 1;

void setup() {
  Serial.begin(9600);
  btSerial.begin(9600);
  randomSeed(analogRead(A0));
  Serial.println(F("Guardian Connect BPM simulator ready."));
}

void loop() {
  // Drift BPM up and down naturally
  bpm += direction * random(0, 2);
  if (bpm > 92) direction = -1;
  if (bpm < 68) direction = 1;

  // Occasional tiny spike/dip
  if (random(0, 10) == 0) bpm += random(-2, 3);
  if (bpm < 55)  bpm = 55;
  if (bpm > 110) bpm = 110;

  Serial.print(F("BPM:"));
  Serial.println(bpm);

  btSerial.print(F("BPM:"));
  btSerial.println(bpm);

  delay(1000);
}
