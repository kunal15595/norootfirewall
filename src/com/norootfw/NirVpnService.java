package com.norootfw;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class NirVpnService extends VpnService implements Runnable {

    private static final int TUN_DEVICE_MTU = 1500;
    /**
     * IP packet size. It includes an IP header, a TCP header, and payload.
     */
    private static final int PACKET_SIZE = 65535;
    /**
     * See http://en.wikipedia.org/wiki/List_of_IP_protocol_numbers for
     * details.
     */
    private static final int TCP_PROTOCOL_NUMBER = 6;

    private static final String TUN_DEVICE_ADDRESS = "10.0.2.1";
    private static final int TRANSPORT_LAYER_PROTOCOL_OFFSET = 9;
    private volatile boolean mThreadRun = true;
    private ParcelFileDescriptor mInterface;
    private Thread mThread;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mThread = new Thread(this, NirVpnService.class.getSimpleName() + "-Thread");
        mThread.start();
        return START_NOT_STICKY;
    }

    @Override
    public void onRevoke() {
        super.onRevoke();
        Log.d("TAG", "onRevoke()");
        try {
            mInterface.close();
        } catch (IOException e) {
            Log.e("TAG", e.getMessage(), e);
        }
    }

    @Override
    public synchronized void run() {
        mInterface = new
                Builder().setSession("FIREWALL_NAME")
                        .addAddress(TUN_DEVICE_ADDRESS, 24)
                        .addRoute("0.0.0.0", 1)
                        .addRoute("128.0.0.0", 1)
                        .setMtu(TUN_DEVICE_MTU)
                        .establish();
        if (mInterface == null) {
            throw new RuntimeException("Failed to create a VPN interface");
        }
        final FileInputStream in = new
                FileInputStream(mInterface.getFileDescriptor());
        final FileOutputStream out = new
                FileOutputStream(mInterface.getFileDescriptor());
        ByteBuffer byteBuffer = ByteBuffer.allocate(PACKET_SIZE);
        while (mThreadRun) {
            try {
                final int tunDeviceRead = in.read(byteBuffer.array());
                if (tunDeviceRead > 0) {
                    /*
                     * If Internet Header Length is greater than 5, there is the
                     * four-byte Options
                     * section that starts at octet 20.
                     * http://ru.wikipedia.org/wiki/IPv4.
                     * Since the former four bits of buffer[0] store IHL, and
                     * the latter four bits store
                     * the protocol version. We need to perform these byte
                     * operations to estimate
                     * the IHL and determine which IP version we use.
                     */
                    final int ipVersion = byteBuffer.get(0) >>> 4;
                    if (ipVersion != 4) {
                        Log.w("TAG", "Not handled. IP version: " +
                                ipVersion);
                        break;
                    }
                    switch (byteBuffer.get(TRANSPORT_LAYER_PROTOCOL_OFFSET))
                    {
                    case TCP_PROTOCOL_NUMBER:

                        // Header length in bytes (byte = 32-bit word *4)
                        int result = JNIWrapper.ip_input(byteBuffer);
                        if (result == JNIWrapper.RES_OK) {
                            int structuresCreated =
                                    JNIWrapper.tcp_listen_cb();
                            if (structuresCreated == JNIWrapper.STR_CREATED) {
                                Socket protectedSocket =
                                        SocketChannel.open().socket();
                                if (!protect(protectedSocket)) {
                                    throw new RuntimeException("Failed to protect the socket");
                                }
                                JNIWrapper.set_protected_socket(protectedSocket);
                                byte[] response =
                                        JNIWrapper.tcp_output_cb();
                                out.write(response);
                            }
                        }
                        break;
                    default:
                        Log.w("TAG", "Not handled. Network level protocol code: " + byteBuffer.get(9));
                        break;
                    }
                }
            } catch (IOException e) {
                Log.e("TAG", e.getMessage(), e);
                break;
            }
        }
    }

    @Override
    public void onDestroy() {
        Log.d("TAG", "onDestroy()");
        if (mThread != null) {
            mThreadRun = false;
            mThread.interrupt();
        }
        if (mInterface != null) {
            try {
                mInterface.close();
            } catch (IOException e) {
                Log.e("TAG", e.getMessage(), e);
            }
        }
        super.onDestroy();
    }
}
