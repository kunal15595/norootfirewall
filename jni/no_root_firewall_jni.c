#include "no_root_firewall_jni.h"
#include "lwip/ip.h"
#include "lwip/pbuf.h"
#include "lwip/mem.h"

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

JNIEXPORT jbyteArray JNICALL Java_com_norootfw_NoRootFwNative_sendUdpRequest
  (JNIEnv *env, jclass clazz, jbyteArray packet, jint payload_length) {
	return NULL;
}
