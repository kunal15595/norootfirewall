package com.norootfw.gui;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.norootfw.R;

public class SecuritySettingsActivity extends PreferenceActivity {

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.security_settings_screen);
    }

}
