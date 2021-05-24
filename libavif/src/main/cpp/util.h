//
// Created by mao on 20-4-16.
//

#ifndef DEX_EDITOR_UTIL_H
#define DEX_EDITOR_UTIL_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif


#ifdef _LP64
#define jlong_to_ptr(a) ((void*)(a))
#define ptr_to_jlong(a) ((jlong)(a))
#else
#define jlong_to_ptr(a) ((void*)(int)(a))
#define ptr_to_jlong(a) ((jlong)(int)(a))
#endif

#define ILLEGAL_STATE_EXEPTION "java/lang/IllegalStateException"

void jniThrowException(JNIEnv *env, const char *className, const char *msg);

void throw_IOException(JNIEnv *env, const char *msg);


#ifdef __cplusplus
}
#endif

#endif //DEX_EDITOR_UTIL_H
