#include "com_norootfw_NoRootFwService.h"

/*
 * Class:     com_norootfw_service_NoRootFwService
 * Method:    sendSyn
 * Signature: ([BI)V
 */
JNIEXPORT void JNICALL Java_com_norootfw_service_NoRootFwService_sendSyn
  (JNIEnv *env, jobject obj, jbyteArray packet, jint length) {
	/*
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
	} */
}

/*
 * Class:     com_norootfw_service_NoRootFwService
 * Method:    initNative
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_norootfw_service_NoRootFwService_initNative
  (JNIEnv *env, jobject service) {
	// lwip_init();
	/* PBUF ROM pbufs are used when an application sends data that is
	 located in memory managed by the application.

	 Design and Implementation of the lwIP TCP/IP Stack.
	 */
	// struct pbuf *buf = pbuf_alloc(PBUF_LINK, payload_length, PBUF_RAM);
	/*
	 * Should I call mem_free() here?
	 * Now I'm not sure. I thought I should call it to release the semaphore which is created in
	 * mem_init(), but a function releasing a semaphore is not explicitly called in mem_free().
	 * However, I should guarantee that the semaphore opened in mem_init() is finally released.
	 *
	 * Maksim Dmitriev
	 * January 8, 2015
	 */
}

/*
 * Class:     com_norootfw_service_NoRootFwService
 * Method:    sendAck
 * Signature: ([BI)V
 */
JNIEXPORT void JNICALL Java_com_norootfw_service_NoRootFwService_sendAck
  (JNIEnv *env, jobject service, jbyteArray packet, jint length) {

}

/*
 * Class:     com_norootfw_service_NoRootFwService
 * Method:    sendRequest
 * Signature: ([BLjava/net/Socket;)V
 */
JNIEXPORT void JNICALL Java_com_norootfw_service_NoRootFwService_sendRequest
  (JNIEnv *env, jobject obj1, jbyteArray packet, jobject obj2) {

}
