#include <jni.h>

#include <stdio.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <termios.h>

#include "SerialPort.h"
#include "android/log.h"

static const char *TAG = "serial_port";
#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO,  TAG, fmt, ##args)
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, ##args)

static speed_t getBaudrate(jint baudrate) {
	switch (baudrate) {
	case 0:
		return B0;
	case 50:
		return B50;
	case 75:
		return B75;
	case 110:
		return B110;
	case 134:
		return B134;
	case 150:
		return B150;
	case 200:
		return B200;
	case 300:
		return B300;
	case 600:
		return B600;
	case 1200:
		return B1200;
	case 1800:
		return B1800;
	case 2400:
		return B2400;
	case 4800:
		return B4800;
	case 9600:
		return B9600;
	case 19200:
		return B19200;
	case 38400:
		return B38400;
	case 57600:
		return B57600;
	case 115200:
		return B115200;
	case 230400:
		return B230400;
	case 460800:
		return B460800;
	case 500000:
		return B500000;
	case 576000:
		return B576000;
	case 921600:
		return B921600;
	case 1000000:
		return B1000000;
	case 1152000:
		return B1152000;
	case 1500000:
		return B1500000;
	case 2000000:
		return B2000000;
	case 2500000:
		return B2500000;
	case 3000000:
		return B3000000;
	case 3500000:
		return B3500000;
	case 4000000:
		return B4000000;
	default:
		return -1;
	}
}

static void set_data_bits(struct termios *termios_new, jint dateLength) {
	(*termios_new).c_cflag &= ~CSIZE;
	switch (dateLength) {
	case 7:
		termios_new->c_cflag |= CS7;
		break;
	case 8:
		termios_new->c_cflag |= CS8;
		break;
	default:
		termios_new->c_cflag |= CS8;
		break;
	}
}

static void set_checking_mode(struct termios *termios_new,
		const char *verifyMode) {
	if (!strcmp("None", verifyMode)) {
		termios_new->c_cflag &= ~PARENB;
		termios_new->c_iflag &= ~INPCK;
		LOGD("set verify mode is None");
	} else if (!strcmp("Even", verifyMode)) {
		termios_new->c_cflag |= PARENB;
		termios_new->c_cflag &= ~PARODD;
		termios_new->c_iflag |= INPCK;
	} else if (!strcmp("Odd", verifyMode)) {
		termios_new->c_cflag |= PARENB;
		termios_new->c_cflag |= PARODD;
		termios_new->c_iflag |= INPCK;
	} else if (!strcmp("Space", verifyMode)) {
		termios_new->c_cflag &= ~PARENB;
	}

	LOGD("Current verify mode is %s", verifyMode);
}

static void set_flow_mode(struct termios *termios_new, const char *flowMode) {
	if (!strcmp("None", flowMode)) {
		termios_new->c_cflag &= ~CRTSCTS;
	} else if (!strcmp("RTS/CTS", flowMode)) {
		termios_new->c_cflag |= CRTSCTS;
	} else if (!strcmp("XON/XOFF", flowMode)) {
		termios_new->c_cflag |= IXON | IXOFF | IXANY;
	} else {
		termios_new->c_cflag &= ~CRTSCTS;
	}
}

static void set_stop_bits(struct termios * termios_new, jint stopBit) {
	switch (stopBit) {
	case 1:
		termios_new->c_cflag &= ~CSTOPB;
		break;
	case 2:
		termios_new->c_cflag |= CSTOPB;
		break;
	default:
		termios_new->c_cflag &= ~CSTOPB;
		break;
	}
}
/*
 * Class:     com_simware_serialchat_SerialPort
 * Method:    native_open
 * Signature: (Ljava/lang/String;IILjava/lang/String;ILjava/lang/String;)Ljava/io/FileDescriptor;
 */
JNIEXPORT jobject JNICALL Java_com_obm_mylibrary_SerialPortModel_open(
		JNIEnv *env, jclass thiz, jstring path, jint baudrate, jint dataBits,
		jstring checkingMode, jint stopBits, jstring flowMode) {

		//Get baudrate
		baudrate = getBaudrate(baudrate);
		if (baudrate == -1) {
			LOGE("Error:Illegal Butyrate %d",baudrate);
			return NULL;
		}//<===========

		//Open port
		const char *pathStr = env->GetStringUTFChars(path,NULL);
		int fd = open(pathStr,O_RDWR | O_NOCTTY);
		if(fd < 0) {
			LOGE("Error:Could not open %s",pathStr);
			env->ReleaseStringUTFChars(path,pathStr);
			return NULL;
		}
		env->ReleaseStringUTFChars(path,pathStr);
		//<=============

		//Configure device
		const char *strCheckingMode = env->GetStringUTFChars(checkingMode,NULL);
		const char *strFlowMode = env->GetStringUTFChars(flowMode,NULL);

	    struct termios tio;
	    if (tcgetattr(fd, &tio))
	        memset(&tio, 0, sizeof(tio));

	    cfmakeraw(&tio);
	    tio.c_cflag =  baudrate | CLOCAL | CREAD;//set baud rate and enable read port.
	    tio.c_oflag &= ~OPOST;//Raw output.
	    tio.c_lflag &= ~(ICANON | ECHO | ECHOE | ISIG);/* turn of CANON, ECHO*, etc */

	    set_data_bits(&tio,dataBits);
	    set_flow_mode(&tio,strFlowMode);
	    set_stop_bits(&tio,stopBits);
	    set_checking_mode(&tio,strCheckingMode);

	    env->ReleaseStringUTFChars(checkingMode, strCheckingMode);
	    env->ReleaseStringUTFChars(flowMode, strFlowMode);

	    tcflush(fd,TCIFLUSH);
	    tio.c_cc[VTIME] = 1;//uint:100ms
	    tio.c_cc[VMIN] = 0;
	    tcflush (fd, TCIFLUSH);
	    tcsetattr(fd,TCSANOW,&tio);
	    //<====================

	    //Create FileDesccriptor for java code
		jclass cFileDescriptor = env->FindClass("java/io/FileDescriptor");
		jmethodID iFileDescriptor = env->GetMethodID(cFileDescriptor,"<init>", "()V");
		jfieldID descriptorID = env->GetFieldID(cFileDescriptor,"descriptor", "I");
		jobject mFileDescriptor = env->NewObject(cFileDescriptor,iFileDescriptor);
		env->SetIntField(mFileDescriptor, descriptorID, (jint) fd);
		//<=====================

	    //Now return FileDescriptor to java code
		return mFileDescriptor;
}

/*
 * Class:     com_simware_serialchat_SerialPort
 * Method:    native_close
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_obm_mylibrary_SerialPortModel_close(
		JNIEnv *env, jobject thiz) {
		jclass SerialPortClass = env->GetObjectClass(thiz);
		jclass FileDescriptorClass = env->FindClass("java/io/FileDescriptor");

		jfieldID mFdID = env->GetFieldID(SerialPortClass, "mFd","Ljava/io/FileDescriptor;");
		jfieldID descriptorID = env->GetFieldID(FileDescriptorClass,"descriptor", "I");

		jobject mFd = env->GetObjectField(thiz, mFdID);
		jint descriptor = env->GetIntField(mFd, descriptorID);

		LOGD("close fd = %d", descriptor);
		close(descriptor);
}
