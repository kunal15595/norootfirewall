package com.norootfw;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class NoRootFwService extends VpnService implements Runnable {

    private static final int TUN_DEVICE_ADDRESS_PREFIX_LENGTH = 24;
    private static final int ROUTE_PREFIX_LENGTH = 1;
    private static final String ROUTE_2 = "128.0.0.0";
    private static final String ROUTE_1 = "0.0.0.0";
    private static final int IP_PACKET_MAX_LENGTH = 65535;
    private static volatile boolean sServiceRun;
    public static final String ACTION_SERVICE_STARTED = "com.norootfw.intent.action.SERVICE_STARTED";

    static {
        System.loadLibrary("lwip");
    }

    private static final String TAG = NoRootFwService.class.getSimpleName();
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
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(ACTION_SERVICE_STARTED));
        NoRootFwNative.initNative();
        // Packets to be sent are queued in this input stream.
        FileInputStream in = null;
        // Packets received need to be written to this output stream.
        FileOutputStream out = null;
        try {
            Log.i(TAG, "Starting");
            in = new FileInputStream(mInterface.getFileDescriptor());
            out = new FileOutputStream(mInterface.getFileDescriptor());
            ByteBuffer byteBuffer = ByteBuffer.allocate(IP_PACKET_MAX_LENGTH);
            while (sServiceRun) {
                // it DOESN'T null out values of the buffer's byte array
                byteBuffer.clear();
                final int read = in.read(byteBuffer.array());
                if (read > 0) {
                    final int packetSize = IPPacket.convertMultipleBytesToPositiveInt(byteBuffer.array()[IPPacket.IP_TOTAL_LENGTH_HIGH_BYTE_INDEX], byteBuffer.array()[IPPacket.IP_TOTAL_LENGTH_LOW_BYTE_INDEX]);
                    byte[] packet = new byte[packetSize];
                    byteBuffer.get(packet);
                    IPPacket.PACKET.setPacket(packet);
                    /*
                     * I used to think that DNS requests are
                     * not performed, but it turned out they were.
                     * 
                     * Maksim Dmitriev
                     * January 9, 2015
                     */
                    if (true) {
                        final byte protocol = IPPacket.PACKET.getProtocol();
                        Log.d(TAG, "DST port: " + IPPacket.PACKET.getDstPort() +
                                " Transport-layer protocol: " + protocol +
                                (protocol == IPPacket.TRANSPORT_PROTOCOL_TCP ?
                                        " TCP flags: " +
                                                IPPacket.PACKET.getPacket()[IPPacket.PACKET.getIpHeaderLength() + IPPacket.TCP_FLAGS_INDEX] : "") +
                                " SRC: " + IPPacket.PACKET.getSrcIpAddressAsString() +
                                " DST: " + IPPacket.PACKET.getDstIpAddressAsString());
                    }
                    if (IPPacket.PACKET.getDstPort() == IPPacket.DST_PORT_DNS) {
                        // TODO: create a protected socket and perform a DNS request
                    }
                    switch (IPPacket.PACKET.getProtocol()) {
                    case IPPacket.TRANSPORT_PROTOCOL_TCP:
                        if (IPPacket.PACKET.isSyn()) {
                            // TODO: receive SYN+ACK and write it to the TUN device
                            NoRootFwNative.sendSyn(IPPacket.PACKET.getPacket(),
                                    IPPacket.PACKET.getPayloadLength());
                        }
                        break;
                    case IPPacket.TRANSPORT_PROTOCOL_UDP:
                        if (IPPacket.PACKET.getDstIpAddressAsString().equals("192.168.1.197")) {
                            Log.d(TAG, "Stop here");
                            DatagramSocket datagramSocket = new DatagramSocket();

                            // TODO: delete the value
                            int testResponseLen = 6;
                            if (protect(datagramSocket)) {

                                /*
                                 * Handle an IOException if anything goes wrong with a data transfer
                                 * done by the protected socket.
                                 * 
                                 * Maksim Dmitriev
                                 * April 12, 2015
                                 */
                                try {
                                    DatagramPacket request = new DatagramPacket(IPPacket.PACKET.getPayload(),
                                            IPPacket.PACKET.getPayload().length,
                                            Inet4Address.getByAddress(IPPacket.PACKET.getDstIpAddress()),
                                            IPPacket.PACKET.getDstPort());
                                    datagramSocket.send(request);

                                    // Test. 1024 bytes
                                    byte[] responseData = new byte[testResponseLen];
                                    DatagramPacket response = new DatagramPacket(responseData, responseData.length);
                                    datagramSocket.receive(response);
                                    IPPacket.PACKET.swapIpAddresses();
                                    Log.d(TAG, "Test response: " + new String(responseData));

                                    int ipHeader = IPPacket.PACKET.getIpHeaderLength();
                                    int transportHeader = IPPacket.PACKET.getTransportLayerHeaderLength();
                                    int headerLengths = ipHeader + transportHeader;
                                    IPPacket.PACKET.setTotalLength(headerLengths + testResponseLen);
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
                                    IPPacket.PACKET.calculateUdpCheckSum();
                                    /*
                                     * Before computing the checksum of the UDP header and data:
                                     * 1. Swap the port numbers.
                                     * 2. Set the response data (setPayload).
                                     * 3. Set the UDP header and data length.
                                     */
                                    byte []toOut = IPPacket.PACKET.getPacket(); // I can see this value in the debugger. It's not used and can be deleted
                                    out.write(IPPacket.PACKET.getPacket(), 0, IPPacket.PACKET.getTotalLength());
                                } catch (IOException e) {
                                    Log.e(TAG, "", e);
                                } finally {
                                    datagramSocket.close();
                                }
                            } else {
                                throw new IllegalStateException("Failed to create a protected socket");
                            }
                        }
                        break;
                    default:
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
        static final int TCP_DATA_OFFSET_INDEX = 12;
        /**
         * The index of the octet of the TCP header where the flags, such as SYN, ACK, are stored.
         */
        static final int TCP_FLAGS_INDEX = 13;
        static final int TRANSPORT_LAYER_DST_PORT_HIGH_BYTE_INDEX = 2;
        static final int TRANSPORT_LAYER_SPACE_IN_BYTES = 2;
        static final int TRANSPORT_LAYER_DST_PORT_LOW_BYTE_INDEX = 3;
        static final int TRANSPORT_LAYER_HEADER_DATA_LENGTH_INDEX = 4;
        static final int TCP_CHECKSUM_1 = 7;
        /**
         * A UDP header length
         */
        static final int UDP_HEADER_LENGTH = 8;

        /**
         * The number of bytes needed to store an IP address
         */
        static final int IP_ADDRESS_LENGTH = 4;
        static final int IP_HEADER_LENGTH_INDEX = 0;
        static final int IP_TOTAL_LENGTH_HIGH_BYTE_INDEX = 2;
        static final int IP_TOTAL_LENGTH_LOW_BYTE_INDEX = 3;
        static final int IP_PROTOCOL_FIELD = 9;
        static final int IP_CHECKSUM_1 = 10;
        static final int IP_CHECKSUM_2 = 11;
        static final int IP_SRC_IP_ADDRESS_INDEX = 12;
        static final int IP_DST_IP_ADDRESS_INDEX = 16;

        static final String DOT = ".";

        // Transport-layer protocols
        static final byte TRANSPORT_PROTOCOL_TCP = 6;
        static final byte TRANSPORT_PROTOCOL_UDP = 17;

        // Destination ports
        static final int DST_PORT_DNS = 53;

        byte[] mPacket;
        byte[] mPayload;
        byte[] mSrcIpAddress;
        byte[] mDstIpAddress;

        void setPacket(byte[] packet) {
            /*
             * I don't think there is a memory leak; therefore, I don't null out mPacket before
             * assigning a new value.
             */
            mPacket = packet;
            mPayload = Arrays.copyOfRange(mPacket, getIpHeaderLength() + getTransportLayerHeaderLength(), mPacket.length);
            mSrcIpAddress = Arrays.copyOfRange(mPacket, IP_SRC_IP_ADDRESS_INDEX, IP_SRC_IP_ADDRESS_INDEX + IP_ADDRESS_LENGTH);
            mDstIpAddress = Arrays.copyOfRange(mPacket, IP_DST_IP_ADDRESS_INDEX, IP_DST_IP_ADDRESS_INDEX + IP_ADDRESS_LENGTH);
        }

        /**
         * Reset {@link #mPacket} and all the other fields initialized in {@link #setPacket(byte[])}
         * not to see them during the next iteration. Call this method when
         */
        private void reset() {
            mPacket = null;
            mPayload = null;
            mSrcIpAddress = null;
            mDstIpAddress = null;
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
            return convertMultipleBytesToPositiveInt(mPacket[IP_TOTAL_LENGTH_HIGH_BYTE_INDEX],
                    mPacket[IP_TOTAL_LENGTH_LOW_BYTE_INDEX]);
        }

        byte getProtocol() {
            return mPacket[IP_PROTOCOL_FIELD];
        }

        byte[] getPayload() {
            return mPayload;
        }

        /**
         * 
         * @param headerLengths it's an offset
         * @param payload
         */
        void setPayload(int headerLengths, byte[] payload) {
            System.arraycopy(payload, 0, mPacket, headerLengths, payload.length);
            mPayload = Arrays.copyOfRange(mPacket, getIpHeaderLength() + getTransportLayerHeaderLength(), mPacket.length);
        }
        
        private void setUdpHeaderAndDataLength(int length) {
            int offset = getIpHeaderLength();
            byte []lengthAsArray = convertPositiveIntToBytes(length);
            mPacket[offset + TRANSPORT_LAYER_HEADER_DATA_LENGTH_INDEX] = lengthAsArray[0];
            mPacket[offset + TRANSPORT_LAYER_HEADER_DATA_LENGTH_INDEX + 1] = lengthAsArray[1];
        }

        private static int convertMultipleBytesToPositiveInt(byte... bytes) {
            int value = convertByteToPositiveInt(bytes[0]) << 8;
            value += convertByteToPositiveInt(bytes[1]);
            return value;
        }
        
        static byte[] convertPositiveIntToBytes(int src) {
            byte[] result = new byte[2];
            result[1] = (byte) (255 & src);
            result[0] = (byte) (255 & (src >>> 8));
            return result;
        }

        /**
         * Since byte is a signed type in Java, its positive value cannot be greater than 127.
         * 
         * @param value
         * @return
         */
        private static int convertByteToPositiveInt(byte value) {
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

        int getTransportLayerHeaderLength() {
            final int transportLayerHeaderStart = getIpHeaderLength();
            final int protocol = getProtocol();
            switch (protocol) {
            case TRANSPORT_PROTOCOL_TCP:
                final int dataOffsetOctet = transportLayerHeaderStart + TCP_DATA_OFFSET_INDEX;
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
            case TRANSPORT_PROTOCOL_UDP:
                return UDP_HEADER_LENGTH;
            default:
                Log.w(TAG, "Unsupported protocol == " + protocol);
                return 0;
            }
        }

        /**
         * Payload without TCP and IP headers
         *
         * @return
         */
        int getPayloadLength() {
            return getTotalLength() - getIpHeaderLength() - getTransportLayerHeaderLength();
        }

        void setTotalLength(int length) {
            byte[] lengthAsBytes = convertPositiveIntToBytes(length);
            mPacket[IP_TOTAL_LENGTH_HIGH_BYTE_INDEX] = lengthAsBytes[0];
            mPacket[IP_TOTAL_LENGTH_LOW_BYTE_INDEX] = lengthAsBytes[1];
        }

        boolean isSyn() {
            return (mPacket[getIpHeaderLength() + TCP_FLAGS_INDEX] & TCP_FLAGS_SYN_MASK) != 0;
        }

        /**
         * 
         * @return The IPv4 address in dot-decimal notation
         */
        private String getIpAddressAsAstring(byte[] addressAsBytes) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < addressAsBytes.length; i++) {
                builder.append(convertByteToPositiveInt(addressAsBytes[i]));
                if (i != addressAsBytes.length - 1) {
                    builder.append(DOT);
                }
            }
            return builder.toString();
        }

        String getSrcIpAddressAsString() {
            return getIpAddressAsAstring(mSrcIpAddress);
        }

        String getDstIpAddressAsString() {
            return getIpAddressAsAstring(mDstIpAddress);
        }

        byte[] getDstIpAddress() {
            return mDstIpAddress;
        }

        int getDstPort() {
            final int tcpHeaderStart = getIpHeaderLength();
            return convertMultipleBytesToPositiveInt(
                    mPacket[tcpHeaderStart + TRANSPORT_LAYER_DST_PORT_HIGH_BYTE_INDEX],
                    mPacket[tcpHeaderStart + TRANSPORT_LAYER_DST_PORT_LOW_BYTE_INDEX]);
        }

        void swapPortNumbers() {
            swapRanges(getIpHeaderLength(), TRANSPORT_LAYER_SPACE_IN_BYTES);
        }
        
        void swapIpAddresses() {
            swapRanges(IP_SRC_IP_ADDRESS_INDEX, IP_ADDRESS_LENGTH);
            mSrcIpAddress = Arrays.copyOfRange(mPacket, IP_SRC_IP_ADDRESS_INDEX, IP_SRC_IP_ADDRESS_INDEX + IP_ADDRESS_LENGTH);
            mDstIpAddress = Arrays.copyOfRange(mPacket, IP_DST_IP_ADDRESS_INDEX, IP_DST_IP_ADDRESS_INDEX + IP_ADDRESS_LENGTH);
        }
        
        private void swapRanges(int start, int length) {
            for (int i = 0; i < length; i++) {
                byte temp = 0;
                temp = mPacket[start];
                int secondRangeStart = start + length;
                mPacket[start] = mPacket[secondRangeStart];
                mPacket[secondRangeStart] = temp;
                start++;
                secondRangeStart++;
            }
        }
        
        void calculateIpHeaderCheckSum() {
             byte [] ipv4Header = Arrays.copyOfRange(mPacket, 0, getIpHeaderLength());
             // Assign 0 values to the checksum bytes themselves
             ipv4Header[IP_CHECKSUM_1] = 0;
             ipv4Header[IP_CHECKSUM_2] = 0;
             long checksum = calculateChecksum(ipv4Header);
             
             /*
              * This casting is safe because we only need two low bytes.
              * 
              * Maksim Dmitriev
              * April 14, 2015
              */
             byte []checksumAsArray = convertPositiveIntToBytes((int) checksum);
             System.arraycopy(checksumAsArray, 0, mPacket, IP_CHECKSUM_1, checksumAsArray.length);
        }
        
        void calculateUdpCheckSum() {
            // TODO: if you want to use mPacket.length, you'll need to reallocate it. There sholdn't be old data
            byte [] udpAndData = Arrays.copyOfRange(mPacket, getIpHeaderLength(), mPacket.length);
            long checksum = calculateChecksum(udpAndData);
            
            /*
             * This casting is safe because we only need two low bytes.
             * 
             * Maksim Dmitriev
             * April 14, 2015
             */
            byte []checksumAsArray = convertPositiveIntToBytes((int) checksum);
            System.arraycopy(checksumAsArray, 0, mPacket, TCP_CHECKSUM_1, checksumAsArray.length);
       }

        /**
         * http://stackoverflow.com/a/4114507/1065835
         * 
         * <br>
         * Calculate the Internet Checksum of a buffer (RFC 1071 -
         * http://www.faqs.org/rfcs/rfc1071.html)
         * Algorithm is
         * 1) apply a 16-bit 1's complement sum over all octets (adjacent 8-bit pairs [A,B], final
         * odd length is [A,0])
         * 2) apply 1's complement to this final sum
         *
         * Notes:
         * 1's complement is bitwise NOT of positive value.
         * Ensure that any carry bits are added back to avoid off-by-one errors
         *
         *
         * @param buf The message
         * @return The checksum
         */
        static long calculateChecksum(byte[] buf) {
            int length = buf.length;
            int i = 0;

            long sum = 0;
            long data;

            // Handle all pairs
            while (length > 1) {
                // Corrected to include @Andy's edits and various comments on Stack Overflow
                data = (((buf[i] << 8) & 0xFF00) | ((buf[i + 1]) & 0xFF));
                sum += data;
                // 1's complement carry bit correction in 16-bits (detecting sign extension)
                if ((sum & 0xFFFF0000) > 0) {
                    sum = sum & 0xFFFF;
                    sum += 1;
                }

                i += 2;
                length -= 2;
            }

            // Handle remaining byte in odd length buffers
            if (length > 0) {
                // Corrected to include @Andy's edits and various comments on Stack Overflow
                sum += (buf[i] << 8 & 0xFF00);
                // 1's complement carry bit correction in 16-bits (detecting sign extension)
                if ((sum & 0xFFFF0000) > 0) {
                    sum = sum & 0xFFFF;
                    sum += 1;
                }
            }

            // Final 1's complement value correction to 16-bits
            sum = ~sum;
            sum = sum & 0xFFFF;
            return sum;
        }
    }
}