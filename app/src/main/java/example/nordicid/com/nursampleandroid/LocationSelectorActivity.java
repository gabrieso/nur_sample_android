package example.nordicid.com.nursampleandroid;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class LocationSelectorActivity extends AppCompatActivity {

    private Spinner spinnerWagon, spinnerSection;
    private Button buttonStartScan;

    // Data structures
    private Map<String, List<String>> wagonSectionMap = new HashMap<>();
    private Map<String, List<String>> sectionTagsMap = new HashMap<>();

    private String selectedWagon;
    private String selectedSection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_selector);

        spinnerWagon = findViewById(R.id.spinnerWagon);
        spinnerSection = findViewById(R.id.spinnerSection);
        buttonStartScan = findViewById(R.id.buttonStartScan);

        // Load the plan
        loadInstallationPlan();

        // Populate wagon spinner
        List<String> wagons = new ArrayList<>(wagonSectionMap.keySet());
        ArrayAdapter<String> wagonAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, wagons);
        wagonAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerWagon.setAdapter(wagonAdapter);

        spinnerWagon.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> adapterView,
                                       android.view.View view, int position, long l) {
                selectedWagon = wagons.get(position);
                List<String> sections = wagonSectionMap.get(selectedWagon);
                ArrayAdapter<String> sectionAdapter = new ArrayAdapter<>(LocationSelectorActivity.this,
                        android.R.layout.simple_spinner_item, sections);
                sectionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerSection.setAdapter(sectionAdapter);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> adapterView) {}
        });

        spinnerSection.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> adapterView,
                                       android.view.View view, int position, long l) {
                selectedSection = (String) spinnerSection.getItemAtPosition(position);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> adapterView) {}
        });

        buttonStartScan.setOnClickListener(v -> {
            if (selectedWagon != null && selectedSection != null) {
                List<String> expectedTags = sectionTagsMap.get(selectedWagon + "-" + selectedSection);
                Intent intent = new Intent(LocationSelectorActivity.this, Inventory.class);
                intent.putExtra("wagon", selectedWagon);
                intent.putExtra("section", selectedSection);
                intent.putStringArrayListExtra("expectedTags", new ArrayList<>(expectedTags));
                startActivity(intent);
            }
        });
    }

    private void loadInstallationPlan() {
        try {
            InputStream is = getAssets().open("installation_plan.json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);

            JSONObject obj = new JSONObject(json);
            Iterator<String> wagonIterator = obj.keys();

            while (wagonIterator.hasNext()) {
                String wagon = wagonIterator.next();
                JSONObject sectionsObj = obj.getJSONObject(wagon).getJSONObject("sections");
                List<String> sectionNames = new ArrayList<>();

                Iterator<String> sectionIterator = sectionsObj.keys();
                while (sectionIterator.hasNext()) {
                    String section = sectionIterator.next();
                    sectionNames.add(section);

                    JSONArray tagsArray = sectionsObj.getJSONArray(section);
                    List<String> tags = new ArrayList<>();
                    for (int i = 0; i < tagsArray.length(); i++) {
                        tags.add(tagsArray.getString(i));
                    }
                    sectionTagsMap.put(wagon + "-" + section, tags);
                }
                wagonSectionMap.put(wagon, sectionNames);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
