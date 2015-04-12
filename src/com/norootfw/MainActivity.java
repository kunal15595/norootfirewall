package com.norootfw;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;

public class MainActivity extends Activity implements OnClickListener {

    private static final int ENBLE_FIREWALL_REQ_CODE = 0x01;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.enable_firewall).setOnClickListener(this);
        
        Enumeration<NetworkInterface> nets;
        try {
            nets = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface netint : Collections.list(nets))
                displayInterfaceInformation(netint);
       
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
       
    }
    
    static void displayInterfaceInformation(NetworkInterface netint) throws SocketException {
        Log.d(NoRootFwService.class.getSimpleName(),  netint.getDisplayName());
        Log.d(NoRootFwService.class.getSimpleName(),  netint.getName());
        Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
        for (InetAddress inetAddress : Collections.list(inetAddresses)) {
            Log.d(NoRootFwService.class.getSimpleName(), "InetAddress: " +  inetAddress);
        }
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
