/*
 * Inspired by:
 * 1. https://github.com/appcelerator/hyperloop-android/blob/master/templates/hyperloop.h,
 * 2. http://stackoverflow.com/q/19375984/1065835,
 * 3. http://stackoverflow.com/a/19377588/1065835.
 *
 * Maksim Dmitriev
 * January 8, 2015
 */
#ifndef LOG_H
#define LOG_H

#include <android/log.h>
#include <stdio.h>

#define TAG "NoRootFwService"
// Feel free to change this value if you need to log longer strings
#define LOG_MESSAGE_LENGTH 512
/*
 * The order in terms of verbosity, from least to most is ERROR, WARN, INFO, DEBUG, VERBOSE.
 * Verbose should never be compiled into an application except during development. Debug logs
 * are compiled in but stripped at runtime. Error, warning and info logs are always kept.
 */
#define LOGE(x...) do { \
  char buf[LOG_MESSAGE_LENGTH]; \
  snprintf(buf, LOG_MESSAGE_LENGTH, x); \
  __android_log_print(ANDROID_LOG_ERROR, TAG, "%s | %s:%i", buf, __FILE__, __LINE__); \
} while (0)

#define LOGW(x...) do { \
  char buf[LOG_MESSAGE_LENGTH]; \
  snprintf(buf, LOG_MESSAGE_LENGTH, x); \
  __android_log_print(ANDROID_LOG_WARN, TAG, "%s | %s:%i", buf, __FILE__, __LINE__); \
} while (0)

#define DEBUG 1

#if DEBUG

#define LOGV(x...) do { \
  char buf[LOG_MESSAGE_LENGTH]; \
  snprintf(buf, LOG_MESSAGE_LENGTH, x); \
  __android_log_print(ANDROID_LOG_VERBOSE, TAG, "%s | %s:%i", buf, __FILE__, __LINE__); \
} while (0)

#define LOGD(x...) do { \
  char buf[LOG_MESSAGE_LENGTH]; \
  snprintf(buf, LOG_MESSAGE_LENGTH, x); \
  __android_log_print(ANDROID_LOG_DEBUG, TAG, "%s | %s:%i", buf, __FILE__, __LINE__); \
} while (0)

#define LOGI(x...) do { \
  char buf[LOG_MESSAGE_LENGTH]; \
  snprintf(buf, LOG_MESSAGE_LENGTH, x); \
  __android_log_print(ANDROID_LOG_INFO, TAG, "%s | %s:%i", buf, __FILE__, __LINE__); \
} while (0)

#else
// Disable logs
#define LOGV(...)
#define LOGD(...)
#define LOGI(...)

#endif // DEBUG

#endif // LOG_H
