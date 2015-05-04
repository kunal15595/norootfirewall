package com.norootfw;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class SecuritySettingsActivity extends PreferenceActivity {

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.security_settings_screen);
    }

}
