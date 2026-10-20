package com.example.guardianconnect;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsManager;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * DashboardActivity
 * -----------------------------------------------------------------------------
 * The main screen. Responsibilities:
 *   1. Connect to an HC-05 Bluetooth module that is ALREADY PAIRED in
 *      Android system Bluetooth settings.
 *   2. Read serial data line-by-line and update a BPM display.
 *   3. Let the user set / edit the emergency contact number IN THE APP
 *      (stored in SharedPreferences, so no rebuild is needed to change it).
 *   4. On SOS button press, send an SMS to the saved contact. Uses a
 *      PendingIntent + BroadcastReceiver so we can show a SPECIFIC error
 *      message when sending fails (no service, radio off, etc.).
 *   5. On Emergency Contact button press, open the phone dialer
 *      pre-filled with the saved number.
 */
public class DashboardActivity extends AppCompatActivity {

    // ======================== CONFIGURATION ========================
    private static final String HC05_NAME = "HC-05";
    private static final UUID SPP_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // SharedPreferences
    private static final String PREFS_NAME = "guardian_connect_prefs";
    private static final String KEY_EMERGENCY_CONTACT = "emergency_contact";

    // Broadcast action used by the SMS PendingIntent
    private static final String ACTION_SMS_SENT =
            "com.example.guardianconnect.SMS_SENT";

    // Request codes
    private static final int REQ_BLUETOOTH_PERMS = 1001;
    private static final int REQ_SMS_PERM = 1002;
    private static final int REQ_ENABLE_BT = 2001;

    // ======================== VIEWS ========================
    private TextView tvBpm;
    private TextView tvStatus;
    private TextView tvLastUpdate;
    private View statusDot;
    private Button btnConnect;
    private Button btnSos;
    private Button btnEmergency;
    private TextView tvEmergencyContact;
    private Button btnEditContact;

    // ======================== STATE ========================
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private ConnectedThread connectedThread;
    private volatile boolean isConnected = false;

    private String currentBpm = "--";
    private String emergencyContact = ""; // loaded from SharedPreferences

    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    // Receives the result of SmsManager.sendTextMessage()
    private BroadcastReceiver smsSentReceiver;

    // ======================== LIFECYCLE ========================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Find views
        tvBpm = findViewById(R.id.tvBpm);
        tvStatus = findViewById(R.id.tvStatus);
        tvLastUpdate = findViewById(R.id.tvLastUpdate);
        statusDot = findViewById(R.id.statusDot);
        btnConnect = findViewById(R.id.btnConnect);
        btnSos = findViewById(R.id.btnSos);
        btnEmergency = findViewById(R.id.btnEmergency);
        tvEmergencyContact = findViewById(R.id.tvEmergencyContact);
        btnEditContact = findViewById(R.id.btnEditContact);

        // Bluetooth
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, R.string.msg_bt_not_supported, Toast.LENGTH_LONG).show();
            btnConnect.setEnabled(false);
        }

        // Load the saved emergency contact (if any)
        loadEmergencyContact();
        refreshContactLabel();

        // Register the SMS result receiver. We get back a resultCode from
        // Android telling us whether sending worked — and WHY it didn't.
        smsSentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleSmsSentResult(getResultCode());
            }
        };
        IntentFilter filter = new IntentFilter(ACTION_SMS_SENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(smsSentReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(smsSentReceiver, filter);
        }

        // Wire up buttons
        btnConnect.setOnClickListener(v -> {
            if (isConnected) disconnectBluetooth();
            else onConnectPressed();
        });
        btnSos.setOnClickListener(v -> onSosPressed());
        btnEmergency.setOnClickListener(v -> onEmergencyPressed());
        btnEditContact.setOnClickListener(v -> showEditContactDialog());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnectBluetooth();
        try {
            if (smsSentReceiver != null) unregisterReceiver(smsSentReceiver);
        } catch (IllegalArgumentException ignored) {
        }
    }

    // ======================== EMERGENCY CONTACT (editable) ========================

    private void loadEmergencyContact() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        emergencyContact = prefs.getString(KEY_EMERGENCY_CONTACT, "");
    }

    private void saveEmergencyContact(String number) {
        emergencyContact = number;
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(KEY_EMERGENCY_CONTACT, number)
                .apply();
        refreshContactLabel();
    }

    private void refreshContactLabel() {
        if (TextUtils.isEmpty(emergencyContact)) {
            tvEmergencyContact.setText(R.string.label_not_set);
        } else {
            tvEmergencyContact.setText(emergencyContact);
        }
    }

    private void showEditContactDialog() {
        // Build an EditText inside a padded container so the dialog looks good.
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_PHONE);
        input.setHint(getString(R.string.hint_phone_number));
        input.setText(emergencyContact);
        input.setSelection(input.getText().length());

        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        params.leftMargin = pad;
        params.rightMargin = pad;
        params.topMargin = pad / 2;
        input.setLayoutParams(params);
        container.addView(input);

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_set_contact_title)
                .setMessage(R.string.dialog_set_contact_msg)
                .setView(container)
                .setPositiveButton(R.string.btn_save, (dialog, which) -> {
                    String entered = input.getText().toString().trim().replaceAll("\\s+", "");
                    if (!isValidNumber(entered)) {
                        Toast.makeText(DashboardActivity.this,
                                R.string.msg_invalid_number, Toast.LENGTH_LONG).show();
                        return;
                    }
                    saveEmergencyContact(entered);
                    Toast.makeText(DashboardActivity.this,
                            R.string.msg_contact_saved, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    /**
     * Basic sanity check on the number:
     *  - Must contain at least 7 digits
     *  - May start with '+' followed by digits
     *  - No letters
     */
    private boolean isValidNumber(String n) {
        if (TextUtils.isEmpty(n)) return false;
        String stripped = n.startsWith("+") ? n.substring(1) : n;
        if (stripped.length() < 7) return false;
        for (int i = 0; i < stripped.length(); i++) {
            if (!Character.isDigit(stripped.charAt(i))) return false;
        }
        return true;
    }

    // ======================== BLUETOOTH FLOW ========================

    private void onConnectPressed() {
        if (!hasBluetoothPermissions()) {
            requestBluetoothPermissions();
            return;
        }
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, R.string.msg_bt_not_enabled, Toast.LENGTH_SHORT).show();
            Intent enableBt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBt, REQ_ENABLE_BT);
            return;
        }
        connectToPairedHc05();
    }

    @SuppressLint("MissingPermission")
    private void connectToPairedHc05() {
        Set<BluetoothDevice> paired = bluetoothAdapter.getBondedDevices();
        if (paired == null || paired.isEmpty()) {
            Toast.makeText(this, R.string.msg_hc05_not_paired, Toast.LENGTH_LONG).show();
            return;
        }

        BluetoothDevice target = null;
        for (BluetoothDevice d : paired) {
            String name = d.getName();
            if (name != null && name.equalsIgnoreCase(HC05_NAME)) {
                target = d;
                break;
            }
        }

        if (target == null) {
            showPairedDevicePicker(paired);
            return;
        }
        startConnectionTo(target);
    }

    @SuppressLint("MissingPermission")
    private void showPairedDevicePicker(Set<BluetoothDevice> paired) {
        final BluetoothDevice[] arr = paired.toArray(new BluetoothDevice[0]);
        String[] names = new String[arr.length];
        for (int i = 0; i < arr.length; i++) {
            String n = arr[i].getName();
            names[i] = (n == null ? "Unknown" : n) + "\n" + arr[i].getAddress();
        }
        new AlertDialog.Builder(this)
                .setTitle("Choose paired device")
                .setItems(names, (dialog, which) -> startConnectionTo(arr[which]))
                .setNegativeButton("Cancel", null)
                .show();
    }

    @SuppressLint("MissingPermission")
    private void startConnectionTo(final BluetoothDevice device) {
        updateStatus(getString(R.string.status_connecting), false);
        new Thread(() -> {
            BluetoothSocket socket = null;
            try {
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID);
                bluetoothAdapter.cancelDiscovery();
                socket.connect();

                bluetoothSocket = socket;
                isConnected = true;

                connectedThread = new ConnectedThread(socket);
                connectedThread.start();

                uiHandler.post(() -> {
                    updateStatus(getString(R.string.status_connected), true);
                    btnConnect.setText(R.string.btn_disconnect_bt);
                    Toast.makeText(DashboardActivity.this,
                            "Connected", Toast.LENGTH_SHORT).show();
                });
            } catch (IOException e) {
                try { if (socket != null) socket.close(); } catch (IOException ignored) {}
                isConnected = false;
                uiHandler.post(() -> {
                    updateStatus(getString(R.string.status_disconnected), false);
                    Toast.makeText(DashboardActivity.this,
                            "Connection failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void disconnectBluetooth() {
        isConnected = false;
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
        try { if (bluetoothSocket != null) bluetoothSocket.close(); } catch (IOException ignored) {}
        bluetoothSocket = null;

        uiHandler.post(() -> {
            updateStatus(getString(R.string.status_disconnected), false);
            btnConnect.setText(R.string.btn_connect_bt);
            tvBpm.setText("--");
            tvLastUpdate.setText("Waiting for data…");
        });
    }

    private void updateStatus(String text, boolean connected) {
        tvStatus.setText(text);
        statusDot.setBackgroundResource(
                connected ? R.drawable.dot_connected : R.drawable.dot_disconnected);
    }

    // ======================== READER THREAD ========================
    private class ConnectedThread extends Thread {
        private final BluetoothSocket socket;
        private final BufferedReader reader;
        private volatile boolean running = true;

        ConnectedThread(BluetoothSocket socket) throws IOException {
            this.socket = socket;
            InputStream inputStream = socket.getInputStream();
            this.reader = new BufferedReader(new InputStreamReader(inputStream));
        }

        @Override
        public void run() {
            while (running && !isInterrupted()) {
                try {
                    final String line = reader.readLine();
                    if (line == null) break;
                    final String bpm = parseBpm(line);
                    if (bpm != null) uiHandler.post(() -> updateBpm(bpm));
                } catch (IOException e) {
                    break;
                }
            }
            uiHandler.post(() -> {
                if (isConnected) {
                    Toast.makeText(DashboardActivity.this,
                            "Connection lost", Toast.LENGTH_SHORT).show();
                    disconnectBluetooth();
                }
            });
        }

        void cancel() {
            running = false;
            try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        }
    }

    @Nullable
    private String parseBpm(@NonNull String line) {
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c >= '0' && c <= '9') digits.append(c);
            else if (digits.length() > 0) break;
        }
        return digits.length() == 0 ? null : digits.toString();
    }

    private void updateBpm(String bpm) {
        currentBpm = bpm;
        tvBpm.setText(bpm);
        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                .format(new Date());
        tvLastUpdate.setText("Last update: " + time);
    }

    // ======================== SOS (SMS) ========================

    private void onSosPressed() {
        // 1. Contact must be set first.
        if (TextUtils.isEmpty(emergencyContact)) {
            Toast.makeText(this, R.string.msg_no_contact_set, Toast.LENGTH_LONG).show();
            showEditContactDialog();
            return;
        }
        // 2. Permission must be granted.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS}, REQ_SMS_PERM);
            return;
        }
        // 3. Confirm before sending.
        new AlertDialog.Builder(this)
                .setTitle("Send SOS alert?")
                .setMessage("This will send an emergency SMS to:\n" + emergencyContact
                        + "\n\nContinue?")
                .setPositiveButton("Send", (dialog, which) -> sendSosSms())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sendSosSms() {
        try {
            String message = getString(R.string.sos_text, currentBpm);
            SmsManager sms;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                sms = getSystemService(SmsManager.class);
            } else {
                sms = SmsManager.getDefault();
            }

            // PendingIntent that fires when Android finishes trying to send.
            Intent sentIntent = new Intent(ACTION_SMS_SENT)
                    .setPackage(getPackageName());
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            PendingIntent sentPI = PendingIntent.getBroadcast(
                    this, 0, sentIntent, flags);

            if (message.length() > 160) {
                java.util.ArrayList<String> parts = sms.divideMessage(message);
                java.util.ArrayList<PendingIntent> sentPIs = new java.util.ArrayList<>();
                for (int i = 0; i < parts.size(); i++) sentPIs.add(sentPI);
                sms.sendMultipartTextMessage(emergencyContact, null, parts, sentPIs, null);
            } else {
                sms.sendTextMessage(emergencyContact, null, message, sentPI, null);
            }

            // The actual success/failure toast happens in handleSmsSentResult(),
            // triggered by the broadcast when Android has finished.
            Toast.makeText(this, "Sending…", Toast.LENGTH_SHORT).show();
        } catch (SecurityException se) {
            Toast.makeText(this, R.string.msg_sms_no_permission, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this,
                    getString(R.string.msg_sms_failed) + ": " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Translates the Android SMS result code into a message the user can act on.
     * (This is what makes the difference between "Failed to send" and
     *  "No signal — check your SIM".)
     */
    private void handleSmsSentResult(int resultCode) {
        switch (resultCode) {
            case android.app.Activity.RESULT_OK:
                Toast.makeText(this, R.string.msg_sms_sent, Toast.LENGTH_LONG).show();
                break;
            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                Toast.makeText(this, R.string.msg_sms_generic_failure,
                        Toast.LENGTH_LONG).show();
                break;
            case SmsManager.RESULT_ERROR_NO_SERVICE:
                Toast.makeText(this, R.string.msg_sms_no_service,
                        Toast.LENGTH_LONG).show();
                break;
            case SmsManager.RESULT_ERROR_NULL_PDU:
                Toast.makeText(this, R.string.msg_sms_null_pdu,
                        Toast.LENGTH_LONG).show();
                break;
            case SmsManager.RESULT_ERROR_RADIO_OFF:
                Toast.makeText(this, R.string.msg_sms_radio_off,
                        Toast.LENGTH_LONG).show();
                break;
            default:
                Toast.makeText(this,
                        getString(R.string.msg_sms_failed) + " (code " + resultCode + ")",
                        Toast.LENGTH_LONG).show();
        }
    }

    // ======================== EMERGENCY CALL ========================

    private void onEmergencyPressed() {
        if (TextUtils.isEmpty(emergencyContact)) {
            Toast.makeText(this, R.string.msg_no_contact_set, Toast.LENGTH_LONG).show();
            showEditContactDialog();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_DIAL,
                Uri.parse("tel:" + emergencyContact));
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Cannot open dialer", Toast.LENGTH_SHORT).show();
        }
    }

    // ======================== PERMISSIONS ========================

    private boolean hasBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestBluetoothPermissions() {
        String[] perms;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms = new String[]{
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
            };
        } else {
            perms = new String[]{ Manifest.permission.ACCESS_FINE_LOCATION };
        }
        ActivityCompat.requestPermissions(this, perms, REQ_BLUETOOTH_PERMS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_BLUETOOTH_PERMS) {
            if (allGranted(grantResults)) onConnectPressed();
            else Toast.makeText(this, "Bluetooth permission is required",
                    Toast.LENGTH_LONG).show();
        } else if (requestCode == REQ_SMS_PERM) {
            if (allGranted(grantResults)) onSosPressed();
            else Toast.makeText(this, R.string.msg_sms_no_permission,
                    Toast.LENGTH_LONG).show();
        }
    }

    private boolean allGranted(int[] results) {
        if (results.length == 0) return false;
        for (int r : results) if (r != PackageManager.PERMISSION_GRANTED) return false;
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_ENABLE_BT) {
            if (resultCode == RESULT_OK) onConnectPressed();
            else Toast.makeText(this, R.string.msg_bt_not_enabled,
                    Toast.LENGTH_SHORT).show();
        }
    }
}
