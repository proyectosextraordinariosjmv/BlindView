package me.danielvillena.ocr;

import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.settings, new SettingsFragment()).commit();
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        EditTextPreference apiKeyGPTPref;
        EditTextPreference apiKeyMapsPref;
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            apiKeyGPTPref = (EditTextPreference) findPreference("apiKeyGPT");
            apiKeyMapsPref = (EditTextPreference) findPreference("apiKeyMaps");

            apiKeyGPTPref.setText(MainActivity.CHATGPT_API_KEY);
            apiKeyMapsPref.setText(MainActivity.MAPS_API_KEY);
            apiKeyGPTPref.setSummary("Clave establecida");
            apiKeyMapsPref.setSummary("Clave establecida");
            apiKeyGPTPref.setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
                @Override
                public void onBindEditText(@NonNull EditText editText) {
                    editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
            });
            apiKeyMapsPref.setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
                @Override
                public void onBindEditText(@NonNull EditText editText) {
                    editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
            });
            apiKeyGPTPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                    if (newValue.toString().equals("clave")) {
                        Toast.makeText(getContext(), MainActivity.CHATGPT_API_KEY, Toast.LENGTH_SHORT).show();
                        return false;
                    } else MainActivity.CHATGPT_API_KEY = newValue.toString();
                    return true;
                }
            });
            apiKeyMapsPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                    if (newValue.toString().equals("clave")) {
                        Toast.makeText(getContext(), MainActivity.MAPS_API_KEY, Toast.LENGTH_SHORT).show();
                        return false;
                    } else MainActivity.MAPS_API_KEY = newValue.toString();
                    return true;
                }
            });
        }
    }
}