#include "SetGPIO.h"
#include <stdlib.h>
#include <errno.h>

#include <sys/ioctl.h>
#include <fcntl.h>
#include <android/log.h>

#define LOG_TAG "SetGPIO"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

#define CMD_HIGH 0x40046101
#define CMD_LOW 0x40046102

#define GPIO_OFFSET 0;

JNIEXPORT jint JNICALL Java_com_obm_mylibrary_Open_nativeSetGPIO
    (JNIEnv *env, jobject thiz, jint gpio, jint level){
    int fd = -1;
    int ret = -1;
    int kgpio = gpio + GPIO_OFFSET;
    LOGD("Open qcom-gpio...");

    fd = open("/dev/qcom-gpio", O_RDWR);
    if (fd == -1) {
        LOGE("Open error:%s",strerror(errno));
        return ret;
    }

    if (level) {
        ret = ioctl(fd, CMD_HIGH, &kgpio);
    } else {
        ret = ioctl(fd, CMD_LOW, &kgpio);
    }

    close(fd);
    return ret;
}