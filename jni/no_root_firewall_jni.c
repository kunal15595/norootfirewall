#include "no_root_firewall_jni.h"
#include "lwip/raw.h"
#include "lwip/pbuf.h"
#include "arch/cc.h"

JNIEXPORT jint JNICALL Java_com_norootfw_NoRootFwNative_ip_1input(JNIEnv *env,
		jclass clazz, jbyteArray packet, jint payload_length) {
	struct pbuf *buf = pbuf_alloc(PBUF_IP, payload_length, PBUF_REF); // fails now.
	// TODO: try raw_send()
	return 0;
}
