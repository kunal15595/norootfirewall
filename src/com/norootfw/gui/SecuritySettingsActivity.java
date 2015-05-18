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
    private String[] mFilteringModeValues;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.security_settings_screen);
        mFilteringMode = (ListPreference) findPreference(getString(R.string.filtering_mode_key));
        mFilteringModeValues = getResources().getStringArray(R.array.filtering_mode_values);

        String currentValue = mFilteringMode.getValue();
        updateFilteringModeSummary(currentValue);
        mFilteringMode.setOnPreferenceChangeListener(this);
    }

    private void updateFilteringModeSummary(String currentValue) {
        for (int i = 0; i < mFilteringModeValues.length; i++) {
            if (currentValue.equals(mFilteringModeValues[i])) {
                mFilteringMode.setSummary(getResources().getStringArray(R.array.filtering_mode_entries_summary)[i]);
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.equals(mFilteringMode)) {
            updateFilteringModeSummary((String) newValue);
            return true;
        }
        return false;
    }

}
