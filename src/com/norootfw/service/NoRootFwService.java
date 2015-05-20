package com.norootfw.service;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.norootfw.R;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

/*
 * TODO: create callback methods which you'll call from native code.
 * 
 * 1. onSynAckReceived(byte []synAckPacket): after sendSyn() has finished its job, and a SYN+ACK is created
 * 2.
 */
public class NoRootFwService extends VpnService implements Runnable {

    private static final int TUN_DEVICE_ADDRESS_PREFIX_LENGTH = 24;
    private static final int ROUTE_PREFIX_LENGTH = 1;
    private static final String ROUTE_2 = "128.0.0.0";
    private static final String ROUTE_1 = "0.0.0.0";
    private static final int IP_PACKET_MAX_LENGTH = 65535;
    private static volatile boolean sServiceRun;
    public static final String ACTION_SERVICE_STARTED = "com.norootfw.intent.action.SERVICE_STARTED";
    private static final Intent INTENT_SERVICE_STARTED = new Intent(ACTION_SERVICE_STARTED);
    /*
     * In fact the max. size of data equals to IP_PACKET_MAX_LENGTH - min. IP header length - transport-layer header length
     * But the difference is negligible. So I use IP_PACKET_MAX_LENGTH
     * 
     * Maksim Dmitriev
     * May 11, 2015
     */
    private byte []mResponseBuffer = new byte[IP_PACKET_MAX_LENGTH];

    static {
        System.loadLibrary("lwip");
    }

    static final String TAG = NoRootFwService.class.getSimpleName();
    private static final String TUN_DEVICE_ADDRESS = "10.0.2.1";
    private Thread mThread;
    private ParcelFileDescriptor mInterface;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        stopCapturingPackets();
        // Start a new session by creating a new thread.
        mThread = new Thread(this, "FirewallThread");
        mThread.start();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopCapturingPackets();
    }

    private void stopCapturingPackets() {
        sServiceRun = false;
        if (mThread != null) {
            mThread.interrupt();
        }
    }

    public static boolean isRun() {
        return sServiceRun;
    }

    @Override
    public void run() {
        mInterface = new Builder().setSession(getString(R.string.app_name))
                .addAddress(TUN_DEVICE_ADDRESS, TUN_DEVICE_ADDRESS_PREFIX_LENGTH)
                .addRoute(ROUTE_1, ROUTE_PREFIX_LENGTH)
                .addRoute(ROUTE_2, ROUTE_PREFIX_LENGTH)
                .establish();
        if (mInterface == null) {
            throw new RuntimeException("Failed to create a TUN interface");
        }
        sServiceRun = true;
        LocalBroadcastManager.getInstance(this).sendBroadcast(INTENT_SERVICE_STARTED);
        initNative();
        // Packets to be sent are queued in this input stream.
        FileInputStream in = null;
        // Packets received need to be written to this output stream.
        FileOutputStream out = null;
        try {
            in = new FileInputStream(mInterface.getFileDescriptor());
            out = new FileOutputStream(mInterface.getFileDescriptor());
            ByteBuffer byteBuffer = ByteBuffer.allocate(IP_PACKET_MAX_LENGTH);
            while (sServiceRun) {
                int read = in.read(byteBuffer.array());
                if (read > 0) {
                    // TODO: if IPv6 is possible, let the user know that it's not supported
                    /*
                     * It's a shallow copy.
                     * 
                     * Maksim Dmitriev
                     * May 11, 2015
                     */
                    IPPacket.PACKET.setPacket(byteBuffer.array());
                    int protocol = IPPacket.PACKET.getProtocol();
                    if (isTestDstAddress()) {
                        Log.d(TAG, "DST port: " + IPPacket.PACKET.getDstPort() +
                                " Transport-layer protocol: " +
                                (protocol == IPPacket.TRANSPORT_PROTOCOL_TCP ?
                                        "TCP, flags: " +
                                                IPPacket.PACKET.getPacket()[IPPacket.PACKET.getIpHeaderLength() + IPPacket.TCP_FLAGS_INDEX] : "UDP") +
                                " SRC: " + IPPacket.PACKET.getSrcIpAddressAsString() +
                                " DST: " + IPPacket.PACKET.getDstIpAddressAsString());
                    }
                    switch (protocol) {
                    case IPPacket.TRANSPORT_PROTOCOL_TCP:
                        // TODO
                        if (IPPacket.PACKET.isSyn()) {
                            sendSyn(IPPacket.PACKET.getPacket(), IPPacket.PACKET.getPacket().length);
                        } else if (IPPacket.PACKET.isAck()) {
                            sendAck(IPPacket.PACKET.getPacket(), IPPacket.PACKET.getPacket().length);
                        } else {
                            // TODO: A request itself
                            /* caused a crash
                            Socket protectedSocket = new Socket();
                            if (protect(protectedSocket)) {
                                sendRequest(IPPacket.PACKET.getPacket(), protectedSocket);
                                // TODO: There may be TCP flags I should take into account
                                try {
                                    protectedSocket.close();
                                } catch (IOException e) {}
                            } else {
                                throw new IllegalStateException("Failed to create a protected socket");
                            } */
                        }
                        break;
                    case IPPacket.TRANSPORT_PROTOCOL_UDP:
                        DatagramSocket datagramSocket = new DatagramSocket();
                        if (protect(datagramSocket)) {
                            /*
                             * Handle an IOException if anything goes wrong with a data transfer
                             * done by the protected socket.
                             * 
                             * Maksim Dmitriev
                             * April 12, 2015
                             */
                            try {
                                if (isTestDstAddress()) {
                                    byte []sentPacket = Arrays.copyOfRange(byteBuffer.array(), 0, IPPacket.PACKET.getTotalLength());
                                    Log.d(TAG, "UDP. Sent packet == " + Arrays.toString(sentPacket));   
                                }
                                DatagramPacket request = new DatagramPacket(IPPacket.PACKET.getPayload(),
                                        IPPacket.PACKET.getPayload().length,
                                        Inet4Address.getByAddress(IPPacket.PACKET.getDstIpAddress()),
                                        IPPacket.PACKET.getDstPort());
                                datagramSocket.send(request);
                                
                                DatagramPacket responsePacket = new DatagramPacket(mResponseBuffer, mResponseBuffer.length);
                                datagramSocket.receive(responsePacket);
                                IPPacket.PACKET.swapIpAddresses();
                                byte []responseData = Arrays.copyOfRange(mResponseBuffer, 0, responsePacket.getLength());
                                if (isTestSrcAddress()) {
                                    Log.d(TAG, "UDP response: " + new String(responseData));
                                }
                                int ipHeader = IPPacket.PACKET.getIpHeaderLength();
                                int transportHeader = IPPacket.PACKET.getTransportLayerHeaderLength();
                                int headerLengths = ipHeader + transportHeader;
                                IPPacket.PACKET.setTotalLength(headerLengths + responseData.length);
                                IPPacket.PACKET.setPayload(headerLengths, responseData);
                                /*
                                 * Before computing the checksum of the IP header:
                                 * 
                                 * 1. Swap IP addresses.
                                 * 2. Calculate the total length.
                                 * 3. Identification (later)
                                 */
                                IPPacket.PACKET.calculateIpHeaderCheckSum();
                                
                                IPPacket.PACKET.swapPortNumbers();
                                IPPacket.PACKET.setUdpHeaderAndDataLength(transportHeader + responseData.length);
                                /*
                                 * Before computing the checksum of the UDP header and data:
                                 * 1. Swap the port numbers.
                                 * 2. Set the response data (setPayload).
                                 * 3. Set the UDP header and data length.
                                 */
                                IPPacket.PACKET.updateUdpCheckSum();
                                if (isTestSrcAddress()) {
                                    byte []toTun = Arrays.copyOfRange(IPPacket.PACKET.getPacket(), 0, IPPacket.PACKET.getTotalLength());
                                    Log.d(TAG, "To TUN == " + Arrays.toString(toTun));   
                                }
                                out.write(IPPacket.PACKET.getPacket(), 0, IPPacket.PACKET.getTotalLength());
                            } catch (IOException e) {
                                Log.e(TAG, "", e);
                            } finally {
                                datagramSocket.close();
                            }
                        } else {
                            throw new IllegalStateException("Failed to create a protected UDP socket");
                        }
                        break;
                    }
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
            IPPacket.PACKET.reset();
            Log.i(TAG, "Exiting");
        }
    }
    
    private native void sendSyn(byte[] packet, int length);

    private native void initNative();

    private native void sendAck(byte[] packet, int length);
    
    private native void sendRequest(byte []packet, Socket protectedSocket);
    
    /** 
     * Called from native code.
     */
    private void onSynAckReceived(byte []synAckPacket) {
        // TODO: write it to TUN
    }
    
    /** 
     * Called from native code.
     */
    private void tcpListenCb() {
        // TODO: 
        // After an ACK (step 3 out of 3 of the handshaking process) is processed
    }
    
    /** 
     * Called from native code.
     */
    private void ipOutputCb(byte []response) {
        // TODO: write it to TUN
        // A response itself is ready to be written to the TUN device.
    }
    
    private void tcpSentCb(int seqNumber) {
        // TODO:
        // This method is called from ipOutputCb after it has successfully written the response 
        // to the TUN device.
    }

    private static boolean isTestDstAddress() {
        return IPPacket.PACKET.getDstIpAddressAsString().equals("192.168.1.197");
    }
    
    static boolean isTestSrcAddress() {
        return IPPacket.PACKET.getSrcIpAddressAsString().equals("192.168.1.197");
    }
}