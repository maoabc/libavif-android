

#设置openssl静态库目录
set(DAV1D_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../dav1d)

if (EXISTS ${DAV1D_DIR}/${ANDROID_ABI})
    set(DAV1D_FOUND TRUE)
else ()
    set(DAV1D_FOUND FALSE)
endif ()

add_library(lib_dav1d STATIC IMPORTED)
set_target_properties(lib_dav1d PROPERTIES IMPORTED_LOCATION
        ${DAV1D_DIR}/${ANDROID_ABI}/libdav1d.a)

set(DAV1D_INCLUDE_DIR ${DAV1D_DIR}/include)

set(DAV1D_VERSION 0.8.2)
set(DAV1D_LIBRARY lib_dav1d)

