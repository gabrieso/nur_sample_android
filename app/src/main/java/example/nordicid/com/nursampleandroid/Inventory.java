package example.nordicid.com.nursampleandroid;

import example.nordicid.com.nursampleandroid.data.AppDatabase;
import example.nordicid.com.nursampleandroid.data.ScanDao;
import example.nordicid.com.nursampleandroid.data.ScanEntity;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.nordicid.nurapi.*;
import com.nordicid.tdt.*;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static com.nordicid.nurapi.NurApi.BANK_EPC;
import static com.nordicid.nurapi.NurApi.BANK_TID;
import static com.nordicid.nurapi.NurApi.BANK_USER;
import static com.nordicid.nurapi.NurApi.MAX_EPC_LENGTH;


public class Inventory extends Activity {
    public static final String TAG = "NUR_SAMPLE";

    private NurApi mNurApi;
    private static AccessoryExtension mAccExt;

    private TextView mResultTextView, mStatusTextView, mEPCTextView;
    private TextView progressTextView, expectedTagsTextView;
    private ToggleButton mInvStreamButton;

    private AppDatabase db;
    private ScanDao scanDao;
    private String mUiStatusMsg = "Waiting for start...";
    private String mUiResultMsg = "";
    private String mUiEpcMsg = "";
    private int mUiStatusColor = Color.BLACK;
    private int mUiResultColor = Color.BLACK;
    private int mUiEpcColor = Color.BLACK;

    private boolean mTriggerDown;
    private int mTagsAddedCounter;
    private TextView allScannedTextView;
    private final ArrayList<String> scannedTagsList = new ArrayList<>();

    private Map<String, Boolean> expectedTagsMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        // Initialize UI elements
        mResultTextView = findViewById(R.id.text_result);
        mStatusTextView = findViewById(R.id.text_status);
        mEPCTextView = findViewById(R.id.text_epc);
        mInvStreamButton = findViewById(R.id.toggleButtonInvStream);
        progressTextView = findViewById(R.id.text_progress);
        expectedTagsTextView = findViewById(R.id.text_expected_tags);
        allScannedTextView = findViewById(R.id.text_all_scanned);


        Button resetButton = findViewById(R.id.buttonReset);

        resetButton.setOnClickListener(v -> {
            String wagon = getIntent().getStringExtra("wagon");
            String section = getIntent().getStringExtra("section");

            scanDao.clearScansForSection(wagon, section);

            // Reset in-memory structures
            scannedTagsList.clear();
            for (String epc : expectedTagsMap.keySet()) {
                expectedTagsMap.put(epc, false);
            }

            // Update UI
            updateExpectedTagsUI();
            updateScannedListUI();
            progressTextView.setText("Progress: 0 / " + expectedTagsMap.size());

            Toast.makeText(this, "Progress reset for " + section, Toast.LENGTH_SHORT).show();
        });


        // Initialize DB
        db = AppDatabase.getInstance(this);
        scanDao = db.scanDao();
        // Load wagon and section
        String wagon = getIntent().getStringExtra("wagon");
        String section = getIntent().getStringExtra("section");
        // Load tags from intent into expectedTagsMap
        expectedTagsMap.clear();
        ArrayList<String> expectedTags = getIntent().getStringArrayListExtra("expectedTags");
        if (expectedTags != null) {
            for (String tag : expectedTags) {
                expectedTagsMap.put(tag, false);
            }
        }
        // Load previously scanned tags for this section
        List<ScanEntity> previousScans = scanDao.getScansForSection(wagon, section);
        for (ScanEntity scan : previousScans) {
            scannedTagsList.add(scan.serialAscii);
            if (expectedTagsMap.containsKey(scan.epc)) {
                expectedTagsMap.put(scan.epc, true);
            }
        }
        updateExpectedTagsUI();
        updateScannedListUI();

        // Get NurApi
        mNurApi = MainActivity.GetNurApi();
        mAccExt = MainActivity.GetAccessoryExtensionApi();
        mNurApi.setListener(mNurApiEventListener);

        mInvStreamButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) StartInventoryStream(); else StopInventoryStream();
            showOnUI();
        });

        mTriggerDown = false;
        mTagsAddedCounter = 0;
        showOnUI();
    }
    private void updateScannedListUI() {
        runOnUiThread(() -> {
            StringBuilder sb = new StringBuilder("All Scanned Tags:\n");
            for (String s : scannedTagsList) {
                sb.append(s).append("\n");
            }
            allScannedTextView.setText(sb.toString());
        });
    }

    private String hexToAscii(String hexStr) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < hexStr.length(); i += 2) {
            String str = hexStr.substring(i, i + 2);
            int charCode = Integer.parseInt(str, 16);
            if (charCode >= 32 && charCode <= 126) { // printable characters
                output.append((char) charCode);
            }
        }
        return output.toString().trim();
    }

    private void updateExpectedTagsUI() {
        if (expectedTagsTextView == null) return; // Safe check
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Boolean> entry : expectedTagsMap.entrySet()) {
            sb.append(hexToAscii(entry.getKey()))
                    .append(" : ")
                    .append(entry.getValue() ? "FOUND" : "NOT FOUND")
                    .append("\n");

        }
        expectedTagsTextView.setText(sb.toString());
    }

    private void showOnUI() {
        runOnUiThread(() -> {
            mResultTextView.setText(mUiResultMsg);
            mResultTextView.setTextColor(mUiResultColor);
            mStatusTextView.setText(mUiStatusMsg);
            mStatusTextView.setTextColor(mUiStatusColor);
            mEPCTextView.setText(mUiEpcMsg);
            mEPCTextView.setTextColor(mUiEpcColor);
        });
    }

    private void StartInventoryStream() {
        try {
            mNurApi.setSetupTxLevel(NurApi.TXLEVEL_10); // Adjust as needed
            mNurApi.clearIdBuffer();
            mNurApi.startInventoryStream();
            mTriggerDown = true;
            mUiStatusMsg = "Inventory streaming...";
        } catch (Exception ex) {
            mUiResultMsg = ex.getMessage();
        }
    }

    private void StopInventoryStream() {
        try {
            if (mNurApi.isInventoryStreamRunning()) mNurApi.stopInventoryStream();
            mTriggerDown = false;
            mUiStatusMsg = "Waiting for start...";
        } catch (Exception ex) {
            mUiResultMsg = ex.getMessage();
            mUiResultColor = Color.RED;
        }
    }

    private NurApiListener mNurApiEventListener = new NurApiListener() {
        @Override
        public void inventoryStreamEvent(NurEventInventory event) {
            try {
                if (event.stopped && mTriggerDown) {
                    // If inventory stopped automatically, restart if toggle is still ON
                    mNurApi.startInventoryStream();
                } else if (event.tagsAdded > 0) {
                    NurTagStorage tagStorage = mNurApi.getStorage();

                    for (int x = mTagsAddedCounter; x < mTagsAddedCounter + event.tagsAdded; x++) {
                        NurTag tag = tagStorage.get(x);
                        String epc = NurApi.byteArrayToHexString(tag.getEpc());

                        // Convert EPC to ASCII for UI display
                        String serialNumber = hexToAscii(epc);

                        // Update last scanned tag
                        mUiEpcMsg = "Last Scanned: " + serialNumber;

                        // Add to scanned list if not already present
                        if (!scannedTagsList.contains(serialNumber)) {
                            scannedTagsList.add(serialNumber);
                        }

                        // Validate against expected tags
                        if (expectedTagsMap.containsKey(epc) && !expectedTagsMap.get(epc)) {
                            expectedTagsMap.put(epc, true);
                        }

                        // Save to DB
                        String wagon = getIntent().getStringExtra("wagon");
                        String section = getIntent().getStringExtra("section");

                        ScanEntity entity = new ScanEntity();
                        entity.wagon = wagon;
                        entity.section = section;
                        entity.epc = epc;
                        entity.serialAscii = serialNumber;
                        entity.timestamp = System.currentTimeMillis();
                        entity.isExpected = expectedTagsMap.containsKey(epc);

                        scanDao.insert(entity);
                    }

                    // Update counter
                    mTagsAddedCounter += event.tagsAdded;

                    // Refresh UI for scanned list
                    updateScannedListUI();

                    // Update progress (expected tags)
                    long foundCount = 0;
                    for (Boolean val : expectedTagsMap.values()) if (val) foundCount++;

                    progressTextView.setText("Progress: " + foundCount + " / " + expectedTagsMap.size());
                    updateExpectedTagsUI();

                    // Stop inventory if all expected tags are found
                    if (foundCount == expectedTagsMap.size()) {
                        StopInventoryStream();
                    }

                    // Show updated status
                    showOnUI();
                }
            } catch (Exception ignored) {}
        }

        @Override public void disconnectedEvent() { finish(); }
        @Override public void triggeredReadEvent(NurEventTriggeredRead event) {}
        @Override public void traceTagEvent(NurEventTraceTag event) {}
        @Override public void programmingProgressEvent(NurEventProgrammingProgress event) {}
        @Override public void nxpEasAlarmEvent(NurEventNxpAlarm event) {}
        @Override public void logEvent(int level, String txt) {}
        @Override public void inventoryExtendedStreamEvent(NurEventInventory event) {}
        @Override public void frequencyHopEvent(NurEventFrequencyHop event) {}
        @Override public void epcEnumEvent(NurEventEpcEnum event) {}
        @Override public void deviceSearchEvent(NurEventDeviceInfo event) {}
        @Override public void debugMessageEvent(String event) {}
        @Override public void connectedEvent() {}
        @Override public void clientDisconnectedEvent(NurEventClientInfo event) {}
        @Override public void clientConnectedEvent(NurEventClientInfo event) {}
        @Override public void bootEvent(String event) {}
        @Override public void IOChangeEvent(NurEventIOChange event) {}
        @Override public void autotuneEvent(NurEventAutotune event) {}
        @Override public void tagTrackingScanEvent(NurEventTagTrackingData event) {}
        @Override public void tagTrackingChangeEvent(NurEventTagTrackingChange event) {}
    };
}