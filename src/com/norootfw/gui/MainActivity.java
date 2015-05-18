package com.norootfw.gui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.norootfw.R;
import com.norootfw.service.NoRootFwService;

public class MainActivity extends Activity implements OnClickListener {

    private static final int ENABLE_FIREWALL_REQ_CODE = 0x01;
    private Button mFirewall;

    private static final IntentFilter FILTER_SERVICE_STARTED = new IntentFilter(NoRootFwService.ACTION_SERVICE_STARTED);
    private BroadcastReceiver mServiceStartReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            mFirewall.setEnabled(false);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mFirewall = (Button) findViewById(R.id.enable_firewall);
        mFirewall.setOnClickListener(this);
        findViewById(R.id.security_settings).setOnClickListener(this);
        findViewById(R.id.filtering_activity).setOnClickListener(this);
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mServiceStartReceiver);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mServiceStartReceiver, FILTER_SERVICE_STARTED);
        if (NoRootFwService.isRun()) {
            mFirewall.setEnabled(false);
        } else {
            mFirewall.setEnabled(true);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.enable_firewall:
            Intent intent = NoRootFwService.prepare(this);
            if (intent != null) {
                startActivityForResult(intent, ENABLE_FIREWALL_REQ_CODE);
            } else {
                onActivityResult(ENABLE_FIREWALL_REQ_CODE, RESULT_OK, null);
            }
            break;
        case R.id.security_settings:
            startActivity(new Intent(this, SecuritySettingsActivity.class));
            break;
        case R.id.filtering_activity:
            startActivity(new Intent(this, FilteringListActivity.class));
            break;
        }
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        if (result == RESULT_OK) {
            Intent intent = new Intent(this, NoRootFwService.class);
            startService(intent);
        }
    }
}
