package com.norootfw.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;

public class Utils {

    public static final String IP_ADDRESS_PATTERN =
            "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

    private Utils() {
        throw new AssertionError();
    }

    static InetAddress getIpv4Address() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface netint : Collections.list(interfaces)) {
                Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
                for (InetAddress inetAddress : Collections.list(inetAddresses)) {
                    if (!netint.isLoopback() && netint.isUp() && inetAddress.getHostAddress().matches(IP_ADDRESS_PATTERN)) {
                        return inetAddress;
                    }
                }
            }
        } catch (SocketException e) {}
        return null;
    }
}
