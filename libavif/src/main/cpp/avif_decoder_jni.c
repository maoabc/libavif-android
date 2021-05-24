

//
// Created by mao on 21-4-27.
//

#include <jni.h>
#include <stdio.h>
#include <malloc.h>
#include <android/bitmap.h>

#include "avif/avif.h"

#include "util.h"
#include "utils/log.h"

typedef struct {
    jbyte *bytes;
    //对应java的byte[]
    jobjectArray byteArrayRef;
    //ByteBuffer对象
    jobject byteBufferRef;

} ByteArrayBuffer;

typedef struct {
    avifDecoder *decoder;
    ByteArrayBuffer buffer;
} MyAvifDecoder;

void throwIae(JNIEnv *env, const char *message, int errorCode) {
    char buf[256];
    snprintf(buf, sizeof(buf), "%s, error %d", message, errorCode);
    jniThrowException(env, ILLEGAL_STATE_EXEPTION, buf);
}


static jlong
Java_libavif_AvifDecoder_createDecoderByteArray0(JNIEnv *env, jclass clazz,
                                                 jbyteArray byteArray, jint off,
                                                 jint len) {

    MyAvifDecoder *myDecoder = malloc(sizeof(MyAvifDecoder));

    avifDecoder *decoder = avifDecoderCreate();
    myDecoder->decoder = decoder;
    myDecoder->buffer.bytes = (*env)->GetByteArrayElements(env, byteArray, NULL);
    myDecoder->buffer.byteArrayRef = (*env)->NewGlobalRef(env, byteArray);
    myDecoder->buffer.byteBufferRef = NULL;

    avifResult result = avifDecoderSetIOMemory(decoder,
                                               (const uint8_t *) (myDecoder->buffer.bytes + off),
                                               len);
    if (result != AVIF_RESULT_OK) {
        goto cleanup;
    }
    result = avifDecoderParse(decoder);
    if (result != AVIF_RESULT_OK) {
        jniThrowException(env, ILLEGAL_STATE_EXEPTION, avifResultToString(result));
        goto cleanup;
    }

    return ptr_to_jlong(myDecoder);


    cleanup:
    avifDecoderDestroy(decoder);
    (*env)->ReleaseByteArrayElements(env,
                                     myDecoder->buffer.byteArrayRef,
                                     myDecoder->buffer.bytes, JNI_ABORT);
    (*env)->DeleteGlobalRef(env, myDecoder->buffer.byteArrayRef);
    free(myDecoder);
    return 0;
}

static jlong
Java_libavif_AvifDecoder_createDecoderByteBuffer0(JNIEnv *env, jclass clazz,
                                                  jobject byteBuffer, jint off,
                                                  jint len) {
    MyAvifDecoder *myDecoder = malloc(sizeof(MyAvifDecoder));

    avifDecoder *decoder = avifDecoderCreate();
    myDecoder->decoder = decoder;
    myDecoder->buffer.bytes = (*env)->GetDirectBufferAddress(env, byteBuffer);
    myDecoder->buffer.byteBufferRef = (*env)->NewGlobalRef(env, byteBuffer);
    myDecoder->buffer.byteArrayRef = NULL;

    avifResult result = avifDecoderSetIOMemory(decoder,
                                               (const uint8_t *) (myDecoder->buffer.bytes + off),
                                               len);
    if (result != AVIF_RESULT_OK) {
        goto cleanup;
    }
    result = avifDecoderParse(decoder);
    if (result != AVIF_RESULT_OK) {
        jniThrowException(env, ILLEGAL_STATE_EXEPTION, avifResultToString(result));
        goto cleanup;
    }

    return ptr_to_jlong(myDecoder);


    cleanup:
    avifDecoderDestroy(decoder);
    (*env)->DeleteGlobalRef(env, myDecoder->buffer.byteBufferRef);
    free(myDecoder);
    return 0;
}

static jint
Java_libavif_AvifDecoder_getImageCount0(JNIEnv *env, jclass clazz, jlong n_decoder) {
    MyAvifDecoder *myDecoder = jlong_to_ptr(n_decoder);
    return myDecoder->decoder->imageCount;
}

static jint
Java_libavif_AvifDecoder_getImageIndex0(JNIEnv *env, jclass clazz, jlong n_decoder) {
    MyAvifDecoder *myDecoder = jlong_to_ptr(n_decoder);
    return myDecoder->decoder->imageIndex;
}

static jint
Java_libavif_AvifDecoder_getImageLimit0(JNIEnv *env, jclass clazz, jlong n_decoder) {
    MyAvifDecoder *myDecoder = jlong_to_ptr(n_decoder);
    return myDecoder->decoder->imageCountLimit;
}

static jboolean
Java_libavif_AvifDecoder_nextImage0(JNIEnv *env, jclass clazz, jlong n_decoder) {
    MyAvifDecoder *myDecoder = jlong_to_ptr(n_decoder);
    return avifDecoderNextImage(myDecoder->decoder) == AVIF_RESULT_OK;
}

static jlong
Java_libavif_AvifDecoder_getImage0(JNIEnv *env, jclass clazz, jlong n_decoder) {
    MyAvifDecoder *myDecoder = jlong_to_ptr(n_decoder);
    return ptr_to_jlong(myDecoder->decoder->image);
}


static jint
Java_libavif_AvifDecoder_getFrame0(JNIEnv *env, jclass clazz, jlong n_decoder,
                                   jobject bitmap) {
    MyAvifDecoder *myDecoder = jlong_to_ptr(n_decoder);

    int ret;

    avifResult result;
    AndroidBitmapInfo info;
    void *pixels;
    avifRGBImage rgb;

    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0
        || info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        throwIae(env, "Couldn't get info from Bitmap", ret);
        return 0;
    }

    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
        throwIae(env, "Bitmap pixels couldn't be locked", ret);
        return 0;
    }

    avifRGBImageSetDefaults(&rgb, myDecoder->decoder->image);
    rgb.format = AVIF_RGB_FORMAT_RGBA;
    rgb.depth = 8;
    rgb.rowBytes = rgb.width * avifRGBImagePixelSize(&rgb);
    rgb.pixels = pixels;

    result = avifImageYUVToRGB(myDecoder->decoder->image, &rgb);

    AndroidBitmap_unlockPixels(env, bitmap);

    if (result != AVIF_RESULT_OK) {
        jniThrowException(env, ILLEGAL_STATE_EXEPTION, avifResultToString(result));
    }

    //ms
    return myDecoder->decoder->imageTiming.duration * 1000;
}

static void
Java_libavif_AvifDecoder_reset0(JNIEnv *env, jclass clazz, jlong n_decoder) {
    MyAvifDecoder *myDecoder = jlong_to_ptr(n_decoder);
    avifResult result = avifDecoderReset(myDecoder->decoder);
    if (result != AVIF_RESULT_OK) {
        jniThrowException(env, ILLEGAL_STATE_EXEPTION, avifResultToString(result));
    }
}


static void
Java_libavif_AvifDecoder_destroy0(JNIEnv *env, jclass clazz, jlong n_decoder) {
    MyAvifDecoder *myDecoder = jlong_to_ptr(n_decoder);

    //free byte array
    if (myDecoder->buffer.byteArrayRef != NULL) {
        (*env)->ReleaseByteArrayElements(env,
                                         myDecoder->buffer.byteArrayRef,
                                         myDecoder->buffer.bytes, JNI_ABORT);
        (*env)->DeleteGlobalRef(env, myDecoder->buffer.byteArrayRef);
    }
    //free byte buffer
    if (myDecoder->buffer.byteBufferRef != NULL) {
        (*env)->DeleteGlobalRef(env, myDecoder->buffer.byteBufferRef);
    }
    avifDecoderDestroy(myDecoder->decoder);

    free(myDecoder);
}


static const JNINativeMethod methods[] = {
        {"createDecoderByteArray0",  "([BII)J",                       (void *) Java_libavif_AvifDecoder_createDecoderByteArray0},

        {"createDecoderByteBuffer0", "(Ljava/nio/ByteBuffer;II)J",    (void *) Java_libavif_AvifDecoder_createDecoderByteBuffer0},

        {"nextImage0",               "(J)Z",                          (void *) Java_libavif_AvifDecoder_nextImage0},

        {"getImageCount0",           "(J)I",                          (void *) Java_libavif_AvifDecoder_getImageCount0},

        {"getImageIndex0",           "(J)I",                          (void *) Java_libavif_AvifDecoder_getImageIndex0},

        {"getImageLimit0",           "(J)I",                          (void *) Java_libavif_AvifDecoder_getImageLimit0},

        {"getImage0",                "(J)J",                          (void *) Java_libavif_AvifDecoder_getImage0},

        {"getFrame0",                "(JLandroid/graphics/Bitmap;)I", (void *) Java_libavif_AvifDecoder_getFrame0},

        {"reset0",                   "(J)V",                          (void *) Java_libavif_AvifDecoder_reset0},

        {"destroy0",                 "(J)V",                          (void *) Java_libavif_AvifDecoder_destroy0},

};

#define NELEM(x) ((int) (sizeof(x) / sizeof((x)[0])))

jboolean registerDecoderNativeMethods(JNIEnv *env) {
    jclass clazz = (*env)->FindClass(env, "libavif/AvifDecoder");
    if (clazz == NULL) {
        return JNI_FALSE;
    }
    if ((*env)->RegisterNatives(env, clazz, methods, NELEM(methods)) < 0) {
        return JNI_FALSE;
    }

    return JNI_TRUE;
}