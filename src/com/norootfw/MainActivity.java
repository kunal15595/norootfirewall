package com.norootfw;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

public class MainActivity extends Activity implements OnClickListener {

    private static final int ENBLE_FIREWALL_REQ_CODE = 0x01;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.enable_firewall).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.enable_firewall:
            Intent intent = NoRootFwService.prepare(this);
            if (intent != null) {
                startActivityForResult(intent, ENBLE_FIREWALL_REQ_CODE);
            } else {
                onActivityResult(ENBLE_FIREWALL_REQ_CODE, RESULT_OK, null);
            }
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
