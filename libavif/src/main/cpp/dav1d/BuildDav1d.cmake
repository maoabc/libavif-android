
set(DAV1D_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../dav1d)

#cross file
if (ANDROID_ABI STREQUAL "x86_64")
    set(CROSS_FILE crossfilespatch/x86_64-android.meson)
elseif (ANDROID_ABI STREQUAL "x86")
    set(CROSS_FILE crossfilespatch/i686-android.meson)
elseif (ANDROID_ABI STREQUAL "armeabi-v7a")
    set(CROSS_FILE crossfilespatch/arm-android.meson)
elseif (ANDROID_ABI STREQUAL "arm64-v8a")
    set(CROSS_FILE crossfilespatch/aarch64-android.meson)
else ()
    message(FATAL_ERROR "Unknown abi:" ${ANDROID_ABI})
endif ()


execute_process(COMMAND bash b.sh ${CROSS_FILE} ${ANDROID_ABI} ${DAV1D_DIR}/${ANDROID_ABI}
        WORKING_DIRECTORY ${DAV1D_DIR}
#        RESULT_VARIABLE result
#        OUTPUT_VARIABLE out
        )
#

if (EXISTS ${DAV1D_DIR}/${ANDROID_ABI})
    set(DAV1D_FOUND TRUE)
else ()
    set(DAV1D_FOUND FALSE)
endif ()

add_library(lib_dav1d STATIC IMPORTED)
set_target_properties(lib_dav1d PROPERTIES IMPORTED_LOCATION
        ${DAV1D_DIR}/${ANDROID_ABI}/lib/libdav1d.a)


set(DAV1D_INCLUDE_DIR ${DAV1D_DIR}/${ANDROID_ABI}/include)

set(DAV1D_VERSION 1.0.0)
set(DAV1D_LIBRARY lib_dav1d)

