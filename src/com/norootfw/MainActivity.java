package com.norootfw;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class MainActivity extends Activity implements OnClickListener {

    private static final int ENBLE_FIREWALL_REQ_CODE = 0x01;
    // "192.168.1.165" = -64, -88, 1, -91
    private byte[] mWlanIpAddress = new byte[] {
            -64, -88, 1, -91
    };
    // 217.69.139.200 == Mail.ru
    private byte[] mRemoteAddress = new byte[] {
            -39, 69, -117, -56
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.enable_firewall).setOnClickListener(this);
        findViewById(R.id.send_test_request).setOnClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
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
        case R.id.send_test_request:
            new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        Socket socket = new Socket("time-A.timefreq.bldrdoc.gov", 13);
                        InputStream inputStream = socket.getInputStream();
                        final int read = inputStream.read();
                        if (read > 0) {
                            Log.d("NoRootFwService", "OK. inputStream is not empty: " + read);
                        } else {
                            Log.e("NoRootFwService", "Error. read() returned: " + read);
                        }
                        socket.close();
                        inputStream.close();
                    } catch (UnknownHostException e) {
                        Log.e("NoRootFwService", "Got " + e.toString(), e);
                    } catch (IOException e) {
                        Log.e("NoRootFwService", "Got " + e.toString(), e);
                    }
                }
            }).start();
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
