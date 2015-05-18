package com.norootfw.gui;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;

import com.norootfw.R;

public class SecuritySettingsActivity extends PreferenceActivity implements
        OnPreferenceChangeListener {

    private ListPreference mFilteringMode;
    private ListPreference mConnectionPreferences;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.security_settings_screen);
        mFilteringMode = (ListPreference) findPreference(getString(R.string.filtering_mode_key));
        mConnectionPreferences = (ListPreference) findPreference(getString(R.string.connection_preference_key));

        updateFilteringModeSummary(mFilteringMode,
                getResources().getStringArray(R.array.filtering_mode_values),
                getResources().getStringArray(R.array.filtering_mode_entries_summary));

        updateFilteringModeSummary(mConnectionPreferences,
                getResources().getStringArray(R.array.connection_preference_mode_values),
                getResources().getStringArray(R.array.connection_preference_entries_summary));

        mFilteringMode.setOnPreferenceChangeListener(this);
        mConnectionPreferences.setOnPreferenceChangeListener(this);
    }

    private void updateFilteringModeSummary(ListPreference listPreference, String current, String[] values, String[] summaries) {
        for (int i = 0; i < values.length; i++) {
            if (current.equals(values[i])) {
                listPreference.setSummary(summaries[i]);
                break;
            }
        }
    }

    private void updateFilteringModeSummary(ListPreference listPreference, String[] values, String[] summaries) {
        updateFilteringModeSummary(listPreference, listPreference.getValue(), values, summaries);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.equals(mFilteringMode)) {
            updateFilteringModeSummary((ListPreference) preference,
                    (String) newValue,
                    getResources().getStringArray(R.array.filtering_mode_values),
                    getResources().getStringArray(R.array.filtering_mode_entries_summary));
            return true;
        } else if (preference.equals(mConnectionPreferences)) {
            updateFilteringModeSummary((ListPreference) preference,
                    (String) newValue,
                    getResources().getStringArray(R.array.connection_preference_mode_values),
                    getResources().getStringArray(R.array.connection_preference_entries_summary));
            return true;
        }
        return false;
    }
}
