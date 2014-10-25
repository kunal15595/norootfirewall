LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := lwip

LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/lwip-1.4.1/include

LOCAL_SRC_FILES := \
	./lwip-1.4.1/sys_arch.c \
	./lwip-1.4.1/cmd.c \
	./lwip-1.4.1/netif.c \
	./lwip-1.4.1/api/api_lib.c \
	./lwip-1.4.1/api/api_msg.c \
	./lwip-1.4.1/api/err.c \
	./lwip-1.4.1/api/netbuf.c \
	./lwip-1.4.1/api/netifapi.c \
	./lwip-1.4.1/api/sockets.c \
	./lwip-1.4.1/api/tcpip.c \
	./lwip-1.4.1/core/def.c \
	./lwip-1.4.1/core/dhcp.c \
	./lwip-1.4.1/core/dns.c \
	./lwip-1.4.1/core/init.c \
	./lwip-1.4.1/core/mem.c \
	./lwip-1.4.1/core/memp.c \
	./lwip-1.4.1/core/netif.c \
	./lwip-1.4.1/core/pbuf.c \
	./lwip-1.4.1/core/raw.c \
	./lwip-1.4.1/core/stats.c \
	./lwip-1.4.1/core/sys.c \
	./lwip-1.4.1/core/tcp.c \
	./lwip-1.4.1/core/tcp_in.c \
	./lwip-1.4.1/core/tcp_out.c \
	./lwip-1.4.1/core/timers.c \
	./lwip-1.4.1/core/udp.c \
	./lwip-1.4.1/netif/etharp.c \
	./lwip-1.4.1/core/ipv4/autoip.c \
	./lwip-1.4.1/core/ipv4/icmp.c \
	./lwip-1.4.1/core/ipv4/igmp.c \
	./lwip-1.4.1/core/ipv4/inet.c \
	./lwip-1.4.1/core/ipv4/inet_chksum.c \
	./lwip-1.4.1/core/ipv4/ip.c \
	./lwip-1.4.1/core/ipv4/ip_addr.c \
	./lwip-1.4.1/core/ipv4/ip_frag.c \
	./NoRootFirewall.cpp

LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)
