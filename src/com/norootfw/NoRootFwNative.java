package com.norootfw;

class NoRootFwNative {

    static native int ip_input(byte[] packet, int payloadLength);

}
