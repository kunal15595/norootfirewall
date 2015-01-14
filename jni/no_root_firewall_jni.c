#include "no_root_firewall_jni.h"
#include "lwip/ip.h"
#include "lwip/pbuf.h"
#include "lwip/mem.h"
#include "lwip/netif.h"
#include "log.h"

static struct netif tun0;

JNIEXPORT jbyteArray JNICALL Java_com_norootfw_NoRootFwNative_sendSyn(
		JNIEnv *env, jclass clazz, jbyteArray packet, jint payload_length) {
	/* PBUF ROM pbufs are used when an application sends data that is
	 located in memory managed by the application.

	 Design and Implementation of the lwIP TCP/IP Stack.
	 */
	mem_init();
	struct pbuf *buf = pbuf_alloc(PBUF_LINK, payload_length, PBUF_RAM);
	/*
	 * Should I call mem_free() here?
	 * Now I'm not sure. I thought I should call it to release the semaphore which is created in
	 * mem_init(), but a function releasing a semaphore is not explicitly called in mem_free().
	 * However, I should guarantee that the semaphore opened in mem_init() is finally released.
	 *
	 * Maksim Dmitriev
	 * January 8, 2015
	 */
	return NULL;
}

JNIEXPORT jbyteArray JNICALL Java_com_norootfw_NoRootFwNative_sendUdpRequest(
		JNIEnv *env, jclass clazz, jbyteArray packet, jint payload_length) {
	mem_init();
	struct pbuf *buf = pbuf_alloc(PBUF_LINK, payload_length, PBUF_RAM);
	if (buf == NULL)
	{
		LOGE("pbuf NOT allocated");
	}
	else
	{
		LOGD("pbuf allocated");
		__android_log_print(ANDROID_LOG_DEBUG, "NoRootFwService", "flags: %d, len: %d, tot_len: %d, type: %d", buf->flags, buf->len, buf->tot_len, buf->type);
		ip_input(buf, NULL);
	}

	return NULL;
}

JNIEXPORT jboolean JNICALL Java_com_norootfw_NoRootFwNative_initTun0NetIf
  (JNIEnv *env, jclass clazz) {
    // TODO: Init netif. netif *tun0 = netif_add(&loop_netif, &loop_ipaddr, &loop_netmask, &loop_gw, NULL, netif_loopif_init, ip_input);
	return 0;
}
