cmake_minimum_required(VERSION 3.6)
project(libavif)


add_subdirectory(libavif)

set(SRC_FILES
        avif_decoder_jni.c
        avif_image_jni.c
        util.c
        jni_init.c)


set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fvisibility=hidden")

add_library(avif-jni SHARED ${SRC_FILES})

target_link_libraries(avif-jni jnigraphics avif log)
