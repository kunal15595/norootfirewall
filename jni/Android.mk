LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := lwip

LOCAL_C_INCLUDES := \
    $(LOCAL_PATH)/include \
	$(LOCAL_PATH)/lwip-1.4.1/src/include \
	$(LOCAL_PATH)/lwip-1.4.1/src/include/ipv4

LOCAL_SRC_FILES := \
	./lwip-1.4.1/src/api/api_lib.c \
	./lwip-1.4.1/src/api/api_msg.c \
	./lwip-1.4.1/src/api/err.c \
	./lwip-1.4.1/src/api/netbuf.c \
	./lwip-1.4.1/src/api/netdb.c \
	./lwip-1.4.1/src/api/netifapi.c \
	./lwip-1.4.1/src/api/sockets.c \
	./lwip-1.4.1/src/api/tcpip.c \
	./lwip-1.4.1/src/core/def.c \
	./lwip-1.4.1/src/core/dhcp.c \
	./lwip-1.4.1/src/core/dns.c \
	./lwip-1.4.1/src/core/init.c \
	./lwip-1.4.1/src/core/ipv4/autoip.c \
	./lwip-1.4.1/src/core/ipv4/icmp.c \
	./lwip-1.4.1/src/core/ipv4/igmp.c \
	./lwip-1.4.1/src/core/ipv4/inet.c \
	./lwip-1.4.1/src/core/ipv4/inet_chksum.c \
	./lwip-1.4.1/src/core/ipv4/ip.c \
	./lwip-1.4.1/src/core/ipv4/ip_addr.c \
	./lwip-1.4.1/src/core/ipv4/ip_frag.c \
	./lwip-1.4.1/src/core/mem.c \
	./lwip-1.4.1/src/core/memp.c \
	./lwip-1.4.1/src/core/netif.c \
	./lwip-1.4.1/src/core/pbuf.c \
	./lwip-1.4.1/src/core/raw.c \
	./lwip-1.4.1/src/core/snmp/asn1_dec.c \
	./lwip-1.4.1/src/core/snmp/asn1_enc.c \
	./lwip-1.4.1/src/core/snmp/mib2.c \
	./lwip-1.4.1/src/core/snmp/mib_structs.c \
	./lwip-1.4.1/src/core/snmp/msg_in.c \
	./lwip-1.4.1/src/core/snmp/msg_out.c \
	./lwip-1.4.1/src/core/stats.c \
	./lwip-1.4.1/src/core/sys.c \
	./lwip-1.4.1/src/core/tcp.c \
	./lwip-1.4.1/src/core/tcp_in.c \
	./lwip-1.4.1/src/core/tcp_out.c \
	./lwip-1.4.1/src/core/timers.c \
	./lwip-1.4.1/src/core/udp.c \
	./lwip-1.4.1/src/netif/etharp.c \
	./lwip-1.4.1/src/netif/ethernetif.c \
	./lwip-1.4.1/src/netif/ppp/auth.c \
	./lwip-1.4.1/src/netif/ppp/chap.c \
	./lwip-1.4.1/src/netif/ppp/chpms.c \
	./lwip-1.4.1/src/netif/ppp/fsm.c \
	./lwip-1.4.1/src/netif/ppp/ipcp.c \
	./lwip-1.4.1/src/netif/ppp/lcp.c \
	./lwip-1.4.1/src/netif/ppp/magic.c \
	./lwip-1.4.1/src/netif/ppp/md5.c \
	./lwip-1.4.1/src/netif/ppp/pap.c \
	./lwip-1.4.1/src/netif/ppp/ppp.c \
	./lwip-1.4.1/src/netif/ppp/ppp_oe.c \
	./lwip-1.4.1/src/netif/ppp/randm.c \
	./lwip-1.4.1/src/netif/ppp/vj.c \
	./lwip-1.4.1/src/netif/slipif.c \
	./lwip-1.4.1/src/sys_arch.c \
	./com_norootfw_NoRootFwService.c

LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)
