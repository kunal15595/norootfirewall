#include "no_root_firewall_jni.h"
#include "lwip/ip.h"

JNIEXPORT jint JNICALL Java_com_norootfw_NoRootFwNative_ip_1input(JNIEnv *env,
		jclass clazz, jbyteArray packet) {
	return 0;
}
