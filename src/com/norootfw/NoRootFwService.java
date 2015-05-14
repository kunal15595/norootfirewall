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
                .addAddress("10.0.2.1", 24)
                .addRoute("0.0.0.0", 0)
                .setMtu(1500)
                .establish();
        if (mInterface == null) {
            throw new RuntimeException("Failed to create a TUN interface");
        }
        sServiceRun = true;
        LocalBroadcastManager.getInstance(this).sendBroadcast(INTENT_SERVICE_STARTED);
        NoRootFwNative.initNative();
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
                            throw new IllegalStateException("Failed to create a protected socket");
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
    
    private static boolean isTestDstAddress() {
        return IPPacket.PACKET.getDstIpAddressAsString().equals("192.168.1.197");
    }
    
    private static boolean isTestSrcAddress() {
        return IPPacket.PACKET.getSrcIpAddressAsString().equals("192.168.1.197");
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
        
        static final int UDP_CHECKSUM_1 = 6;
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
        
        // IPv4 pseudo header
        private static final int IPV4_PSEUDO_HEADER_LENGTH = 20;
        static final int IP_PSEUDO_SRC_IP = 0;
        static final int IP_PSEUDO_DST_IP = 4;
        static final int IP_PSEUDO_ZEROS = 8;
        static final int IP_PSEUDO_PROTOCOL = 9;
        // The entity takes two bytes
        static final int IP_PSEUDO_UDP_LENGTH_1 = 10;
        static final int IP_PSEUDO_UDP_HEADER_START = 12;

        byte[] mPacket;
        byte[] mPayload;
        byte[] mSrcIpAddress;
        byte[] mDstIpAddress;
        private byte[] mIpv4PseudoHeader = new byte[IPV4_PSEUDO_HEADER_LENGTH];

        void setPacket(byte[] packet) {
            /*
             * I don't think there is a memory leak; therefore, I don't null out mPacket before
             * assigning a new value.
             */
            mPacket = packet;
            mSrcIpAddress = Arrays.copyOfRange(mPacket, IP_SRC_IP_ADDRESS_INDEX, IP_SRC_IP_ADDRESS_INDEX + IP_ADDRESS_LENGTH);
            mDstIpAddress = Arrays.copyOfRange(mPacket, IP_DST_IP_ADDRESS_INDEX, IP_DST_IP_ADDRESS_INDEX + IP_ADDRESS_LENGTH);
            mPayload = Arrays.copyOfRange(mPacket, getIpHeaderLength() + getTransportLayerHeaderLength(), getTotalLength());
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
            for (int i = 0; i < mIpv4PseudoHeader.length; i++) {
                mIpv4PseudoHeader[i] = 0;
            }
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
            int totalLength = convertMultipleBytesToPositiveInt(mPacket[IP_TOTAL_LENGTH_HIGH_BYTE_INDEX],
                    mPacket[IP_TOTAL_LENGTH_LOW_BYTE_INDEX]);
            return totalLength;
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
            int result = (mPacket[IP_HEADER_LENGTH_INDEX] & IHL_MASK) * BIT_WORD_TO_BYTE_MULTIPLIER;
            return result;
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
        
        void updateUdpCheckSum() {
            // Fill mIpv4PseudoHeader
            System.arraycopy(mPacket, IP_SRC_IP_ADDRESS_INDEX, mIpv4PseudoHeader, IP_PSEUDO_SRC_IP, IP_ADDRESS_LENGTH);
            System.arraycopy(mPacket, IP_DST_IP_ADDRESS_INDEX, mIpv4PseudoHeader, IP_PSEUDO_DST_IP, IP_ADDRESS_LENGTH);
            mIpv4PseudoHeader[IP_PSEUDO_ZEROS] = 0;
            mIpv4PseudoHeader[IP_PSEUDO_PROTOCOL] = TRANSPORT_PROTOCOL_UDP;
            byte []udpLength = new byte[] {
                    mPacket[getIpHeaderLength() + TRANSPORT_LAYER_HEADER_DATA_LENGTH_INDEX],
                    mPacket[getIpHeaderLength() + TRANSPORT_LAYER_HEADER_DATA_LENGTH_INDEX + 1]
            };
            System.arraycopy(udpLength, 0, mIpv4PseudoHeader, IP_PSEUDO_UDP_LENGTH_1, udpLength.length);
            // Copy the UDP header itself without the last two bytes which contain the checksum
            System.arraycopy(mPacket, getIpHeaderLength(), mIpv4PseudoHeader, IP_PSEUDO_UDP_HEADER_START, UDP_HEADER_LENGTH - 2);
            if (isTestSrcAddress()) {
                Log.d(TAG, "mIpv4PseudoHeader == " + Arrays.toString(mIpv4PseudoHeader));   
            }
            long checksum = calculateChecksum(mIpv4PseudoHeader);   
            /*
             * This casting is safe because the value takes two bytes. If it didn't, they would
             * have allocate more space when they were developing the protocol
             * 
             * Maksim Dmitriev
             * May 4, 2015
             */
            byte []checksumAsArray = convertPositiveIntToBytes((int) checksum);
            System.arraycopy(checksumAsArray, 0, mPacket, getIpHeaderLength() + UDP_CHECKSUM_1, checksumAsArray.length);
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