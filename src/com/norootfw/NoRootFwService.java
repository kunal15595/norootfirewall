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
                    IPPacket.PACKET.setPacket(byteBuffer.array());
                    /*
                     * TODO: Move the next line inside IPPacket.PACKET.isSyn() after you find out
                     * the sequence of steps which are taken. I used to think that DNS requests are
                     * not performed, but it turned out they were.
                     *
                     * Maksim Dmitriev
                     * January 9, 2015
                     */
                    NoRootFwNative.sendSyn(IPPacket.PACKET.getPacket(), IPPacket.PACKET.getPayloadLength());
                    if (IPPacket.PACKET.isSyn()) {

                    } else {
                        Log.d(TAG, "Not SYN");
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
            Log.i(TAG, "Exiting");
        }
    }

    /**
     * The implementation of an IP packet is designed as a single-element enum to ensure that there
     * is only one instance of the IP packet.
     * <br />
     * <ul>
     * <li>All the TCP_XXX_INDEX positions are relative to the beginning of a TCP header.</li>
     * <li>All the IP_XXX_INDEX positions are relative to the beginning of an IP header.</li>
     * </ul>
     *
     * @author Maksim Dmitriev
     *
     */
    private static enum IPPacket {

        PACKET;

        /**
         * We multiply the number of 32-bit words by 4 to get the number of bytes.
         */
        static final int BIT_WORD_TO_BYTE_MULTIPLIER = 4;
        static final int IHL_MASK = 0x0f;
        static final int TCP_FLAGS_SYN_MASK = 0x02;
        static final int INTEGER_COMPLEMENT = 256;
        /**
         * The index of the octet of the TCP header where the size of the TCP header is stored;
         */
        static int TCP_DATA_OFFSET_INDEX = 12;
        /**
         * The index of the octet of the TCP header where the flags, such as SYN, ACK, are stored.
         */
        static int TCP_FLAGS_INDEX = 13;
        static int IP_HEADER_LENGTH_INDEX = 0;
        static int IP_TOTAL_LENGTH_HIGH_BYTE_INDEX = 2;
        static int IP_TOTAL_LENGTH_LOW_BYTE_INDEX = 3;

        byte[] mPacket;

        void setPacket(byte[] packet) {
            /*
             * I don't think there is a memory leak; therefore, I don't null out mPacket before
             * assigning a new value.
             */
            mPacket = packet;
        }

        byte[] getPacket() {
            return mPacket;
        }

        /**
         * It <i>does not</i> return the array size. It returns the size of the IP packet in the
         * array.
         * 
         * @return
         */
        int getTotalLength() {
            int length = convertByteToPositiveInt(mPacket[IP_TOTAL_LENGTH_HIGH_BYTE_INDEX]) << 8;
            length += convertByteToPositiveInt(mPacket[IP_TOTAL_LENGTH_LOW_BYTE_INDEX]);
            return length;
        }

        /**
         * Since byte is a signed type in Java, its positive value cannot be greater than 127.
         * 
         * @param value
         * @return
         */
        int convertByteToPositiveInt(byte value) {
            return value >= 0 ? value : value + INTEGER_COMPLEMENT;
        }

        /**
         * Header length in bytes. In this case we don't have to call
         * {@link #convertByteToPositiveInt(byte)} after applying {@link #IHL_MASK} to ensure that
         * a positive number will be multiplied by {@link #BIT_WORD_TO_BYTE_MULTIPLIER}.
         * <br>
         * <br>
         * Consider the following example.
         * <br>
         * <br>
         * 00010110 = 22<br>
         * The ones' complement:<br>
         * 11101001<br>
         * The twos' complement = the ones' complement + 1:<br>
         * 11101001 +<br>
         * 00000001 =<br>
         * 11101010 = -22
         * 
         * <br>
         * <br>
         * So a negative number's high-order bit is always equal to 1, and this bit as well as four
         * high-order bits will always
         * be equal to 0 because the mask we apply in the bitwise AND is equal to 6 = 00000110.<br>
         * 
         * @return the IP header length in bytes
         */
        int getIpHeaderLength() {
            return (mPacket[IP_HEADER_LENGTH_INDEX] & IHL_MASK) * BIT_WORD_TO_BYTE_MULTIPLIER;
        }

        int getTcpHeaderLength() {
            final int tcpHeaderStart = getIpHeaderLength();
            final int dataOffsetOctet = tcpHeaderStart + TCP_DATA_OFFSET_INDEX;
            /*
             * Why do we shift to the right 4 times? Because the value we are interested in is
             * stored in 4 high-order bits.
             * 
             * Why do we multiply the result by 4? Because the value stored in the 4 high-order
             * bytes
             * is the size of the TCP header in 32-bit words, and we need the result in bytes.
             * 
             * Maksim Dmitriev
             * January 8, 2015
             */
            return (convertByteToPositiveInt(mPacket[dataOffsetOctet]) >>> 4) * 4;
        }

        /**
         * Payload without TCP and IP headers
         *
         * @return
         */
        int getPayloadLength() {
            return getTotalLength() - getIpHeaderLength() - getTcpHeaderLength();
        }

        boolean isSyn() {
            return (mPacket[getIpHeaderLength() + TCP_FLAGS_INDEX] & TCP_FLAGS_SYN_MASK) != 0;

        }
    }
}