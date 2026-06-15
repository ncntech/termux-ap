package com.termux.app.nastech;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.termux.R;

/**
 * NasTech AI Terminal — Settings Screen
 * Configure API key, biometric lock, AI model selection.
 */
public class NasTechSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nastech_settings);

        EditText apiKeyInput = findViewById(R.id.nastech_settings_api_key);
        Switch bioSwitch = findViewById(R.id.nastech_settings_biometric);
        Button saveBtn = findViewById(R.id.nastech_settings_save);
        Button backBtn = findViewById(R.id.nastech_settings_back);

        // Load current values
        if (apiKeyInput != null) {
            String key = NasTechManager.getApiKey();
            apiKeyInput.setText(key.isEmpty() ? "" : "••••••••" + key.substring(Math.max(0, key.length() - 4)));
        }

        if (bioSwitch != null) {
            bioSwitch.setChecked(NasTechManager.isBiometricLockEnabled());
            bioSwitch.setOnCheckedChangeListener((btn, isChecked) ->
                NasTechManager.setBiometricLock(isChecked));
        }

        if (saveBtn != null) {
            saveBtn.setOnClickListener(v -> {
                if (apiKeyInput != null) {
                    String key = apiKeyInput.getText().toString().trim();
                    if (!key.startsWith("••") && !key.isEmpty()) {
                        NasTechManager.setApiKey(key);
                    }
                }
                Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
                finish();
            });
        }

        if (backBtn != null) backBtn.setOnClickListener(v -> finish());
    }
}
