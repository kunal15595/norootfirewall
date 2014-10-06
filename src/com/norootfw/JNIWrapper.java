package com.norootfw;

import java.net.Socket;
import java.nio.ByteBuffer;


public class JNIWrapper {

    public static final int RES_OK = 0;
    public static final int STR_CREATED = 0;

    public static void set_protected_socket(Socket protectedSocket) {
        // TODO Auto-generated method stub
        
    }

    public static int tcp_listen_cb() {
        // TODO Auto-generated method stub
        return 0;
    }

    public static int ip_input(ByteBuffer byteBuffer) {
        // TODO Auto-generated method stub
        return 0;
    }

    public static byte[] tcp_output_cb() {
        // TODO Auto-generated method stub
        return null;
    }


}
