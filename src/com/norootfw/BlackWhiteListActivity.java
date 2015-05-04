package com.norootfw;

import android.app.ListActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;

public class BlackWhiteListActivity extends ListActivity {

    private static final String[] MOCK_SETTINGS = new String[] {
            "vk.com",
            "213.180.204.3",
            "93.158.134.3",
            "213.180.193.3"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, MOCK_SETTINGS);
        setListAdapter(adapter);
    }
}
