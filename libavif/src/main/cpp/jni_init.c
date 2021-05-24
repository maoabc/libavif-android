//
// Created by mao on 21-4-29.
//




#include <jni.h>


extern jboolean registerDecoderNativeMethods(JNIEnv *env);

extern jboolean registerImageNativeMethods(JNIEnv *env);

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = NULL;

    if ((*vm)->GetEnv(vm, (void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return -1;
    }
    if (!registerDecoderNativeMethods(env)) {
        return -1;
    }
    if (!registerImageNativeMethods(env)) {
        return -1;
    }

    return JNI_VERSION_1_6;
}

