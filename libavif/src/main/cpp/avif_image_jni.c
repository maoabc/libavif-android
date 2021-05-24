
//
// Created by mao on 21-4-27.
//

#include <jni.h>

#include "avif/avif.h"

#include "util.h"

static jint
Java_libavif_AvifImage_getWidth0(JNIEnv *env, jclass clazz, jlong n_image) {
    avifImage *image = jlong_to_ptr(n_image);
    return image->width;
}

static jint
Java_libavif_AvifImage_getHeight0(JNIEnv *env, jclass clazz, jlong n_image) {
    avifImage *image = jlong_to_ptr(n_image);
    return image->height;
}

static jint
Java_libavif_AvifImage_getDepth0(JNIEnv *env, jclass clazz, jlong n_image) {
    avifImage *image = jlong_to_ptr(n_image);
    return image->depth;
}

static jint
Java_libavif_AvifImage_getYuvFormat0(JNIEnv *env, jclass clazz, jlong n_image) {
    avifImage *image = jlong_to_ptr(n_image);
    return image->yuvFormat;
}

static jint
Java_libavif_AvifImage_getYuvRange0(JNIEnv *env, jclass clazz, jlong n_image) {
    avifImage *image = jlong_to_ptr(n_image);
    return image->yuvRange;
}


static JNINativeMethod methods[] = {
        {"getWidth0",     "(J)I", (void *) Java_libavif_AvifImage_getWidth0},

        {"getHeight0",    "(J)I", (void *) Java_libavif_AvifImage_getHeight0},

        {"getDepth0",     "(J)I", (void *) Java_libavif_AvifImage_getDepth0},

        {"getYuvFormat0", "(J)I", (void *) Java_libavif_AvifImage_getYuvFormat0},

        {"getYuvRange0",  "(J)I", (void *) Java_libavif_AvifImage_getYuvRange0},

};


#define NELEM(x) ((int) (sizeof(x) / sizeof((x)[0])))

jboolean registerImageNativeMethods(JNIEnv *env) {
    jclass clazz = (*env)->FindClass(env, "libavif/AvifImage");
    if (clazz == NULL) {
        return JNI_FALSE;
    }
    if ((*env)->RegisterNatives(env, clazz, methods, NELEM(methods)) < 0) {
        return JNI_FALSE;
    }

    return JNI_TRUE;
}