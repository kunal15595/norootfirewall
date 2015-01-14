package com.norootfw;

class NoRootFwNative {

    /**
     *
     * @param packet an IP packet with the SYN flag
     * @param payloadLength
     * @return an IP packet with SYN+ACK which is returned by lwIP
     */
    static native byte[] sendSyn(byte[] packet, int payloadLength);

    static native byte[] sendUdpRequest(byte[] packet, int payloadLength);

    /**
     * Initialize the network interface tun0, so that lwIP will use it
     *
     * @return true if initialized successfully, false otherwise
     */
    static native boolean initTun0NetIf();
}
