package com.norootfw;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class NoRootFwService extends VpnService implements Runnable {

    private static final int IP_PACKET_MAX_LENGTH = 65535;

    static {
        System.loadLibrary("lwip");
    }

    private static final String TAG = NoRootFwService.class.getSimpleName();
    private static final String TUN_DEVICE_ADDRESS = "10.0.2.1";
    private Thread mThread;
    private ParcelFileDescriptor mInterface;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Stop the previous session by interrupting the thread.
        if (mThread != null) {
            mThread.interrupt();
        }

        // Start a new session by creating a new thread.
        mThread = new Thread(this, "FirewallThread");
        mThread.start();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mThread != null) {
            mThread.interrupt();
        }
    }

    @Override
    public synchronized void run() {
        mInterface = new Builder().setSession("FIREWALL_NAME")
                .addAddress(TUN_DEVICE_ADDRESS, 24)
                .addRoute("0.0.0.0", 1)
                .addRoute("128.0.0.0", 1)
                .establish();
        if (mInterface == null) {
            throw new RuntimeException("Failed to create a TUN interface");
        }
        // Packets to be sent are queued in this input stream.
        FileInputStream in = null;
        // Packets received need to be written to this output stream.
        FileOutputStream out = null;
        try {
            Log.i(TAG, "Starting");
            in = new FileInputStream(mInterface.getFileDescriptor());
            out = new FileOutputStream(mInterface.getFileDescriptor());
            ByteBuffer byteBuffer = ByteBuffer.allocate(IP_PACKET_MAX_LENGTH);
            while (true) {
                final int read = in.read(byteBuffer.array());
                if (read > 0) {
                    NoRootFwNative.ip_input(byteBuffer.array(), IPPacketUtils.getPayloadLength(byteBuffer.array()));
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Got " + e.toString(), e);
        } finally {
            try {
                /*
                 * I'm not quite sure it's necessary to close the streams, but I'll do that
                 * 
                 * Maxim Dmitriev
                 * January 5, 2015
                 */
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
                mInterface.close();
            } catch (IOException e) {
                Log.e(TAG, "Got " + e.toString(), e);
            }
            mInterface = null;
            Log.i(TAG, "Exiting");
        }
    }

    private static class IPPacketUtils {

        static final int IHL_MASK = 0x0f;

        private IPPacketUtils() {
            throw new AssertionError();
        }

        /**
         * It <i>does not</i> return the array size. It returns the size of the IP packet in the
         * array.
         * 
         * @param array
         * @return
         */
        static int getTotalLength(byte[] array) {
            int length = array[2] << 8;
            length += array[3];
            return length;
        }

        /**
         * Header length in bytes
         * 
         * @return
         */
        static int getHeaderLength(byte[] packet) {
            /*
             * We multiply the number of 32-bit words by 4 to get the number of bytes.
             * 
             * Maxim Dmitriev
             * January 5, 2015
             */
            return (packet[0] & IHL_MASK) * 4;
        }

        static int getPayloadLength(byte[] packet) {
            return getTotalLength(packet) - getHeaderLength(packet);
        }
    }
}