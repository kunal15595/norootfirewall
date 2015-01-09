package com.norootfw;

class NoRootFwNative {

    /**
     *
     * @param packet an IP packet with the SYN flag
     * @param payloadLength
     * @return an IP packet with SYN+ACK which is returned by lwIP
     */
    static native byte[] sendSyn(byte[] packet, int payloadLength);

}
